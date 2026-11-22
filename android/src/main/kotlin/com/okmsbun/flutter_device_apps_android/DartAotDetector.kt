package com.okmsbun.flutter_device_apps_android

import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/** Identifies Dart AOT snapshots through defined exports in Android ELF libraries. */
internal object DartAotDetector {
  private val snapshotSymbolPairs = listOf(
    setOf("_kDartSnapshotData", "_kDartSnapshotText"),
    setOf("_kDartIsolateSnapshotData", "_kDartIsolateSnapshotInstructions")
  )
  private val snapshotSymbols = snapshotSymbolPairs.flatten().toSet()
  private val maximumSymbolLength = snapshotSymbols.maxOf { it.length }
  private const val MAX_TABLE_BYTES = 4L * 1024 * 1024

  fun matches(apk: ZipFile, library: ZipEntry): Boolean {
    val header = read(apk, library, 0, 64) ?: return false
    // Android uses little-endian ELF shared objects (ET_DYN).
    if (header.getInt(0) != 0x464c457f || header[5].toInt() != 1 ||
      header[6].toInt() != 1 || header.getShort(16).toInt() != 3
    ) return false
    val is64Bit = when (header[4].toInt()) {
      1 -> false
      2 -> true
      else -> return false
    }

    val sectionHeaderSize = if (is64Bit) 64 else 40
    val firstSectionHeader = header.word(if (is64Bit) 40 else 32, is64Bit)
    val sectionCount = header.unsignedShort(if (is64Bit) 60 else 48)
    if (firstSectionHeader < (if (is64Bit) 64 else 52) ||
      header.unsignedShort(if (is64Bit) 58 else 46) != sectionHeaderSize
    ) return false
    val sections = read(apk, library, firstSectionHeader, sectionHeaderSize.toLong() * sectionCount)
      ?: return false

    fun readSection(offset: Int): ByteBuffer? = read(
      apk, library,
      sections.word(offset + (if (is64Bit) 24 else 16), is64Bit),
      sections.word(offset + (if (is64Bit) 32 else 20), is64Bit)
    )

    val symbolSize = if (is64Bit) 24 else 16
    for (index in 1 until sectionCount) {
      val offset = index * sectionHeaderSize
      if (sections.getInt(offset + 4) != 11 || // SHT_DYNSYM
        sections.word(offset + (if (is64Bit) 56 else 36), is64Bit) != symbolSize.toLong()
      ) continue
      val stringTableIndex = sections.getInt(offset + (if (is64Bit) 40 else 24))
      if (stringTableIndex !in 1 until sectionCount) continue
      val stringsOffset = stringTableIndex * sectionHeaderSize
      if (sections.getInt(stringsOffset + 4) != 3) continue // SHT_STRTAB
      val symbols = readSection(offset) ?: continue
      val strings = readSection(stringsOffset)?.array() ?: continue
      if (hasSnapshotSymbols(symbols, strings, symbolSize)) return true
    }
    return false
  }

  private fun hasSnapshotSymbols(symbols: ByteBuffer, strings: ByteArray, symbolSize: Int): Boolean {
    if (symbols.limit() % symbolSize != 0) return false
    val infoOffset = if (symbolSize == 24) 4 else 12
    val found = mutableSetOf<String>()
    for (offset in symbolSize until symbols.limit() step symbolSize) {
      val binding = symbols[offset + infoOffset].toInt() ushr 4 and 0x0f
      val visibility = symbols[offset + infoOffset + 1].toInt() and 0x07
      val section = symbols.unsignedShort(offset + infoOffset + 2)
      if (binding !in 1..2 || (visibility != 0 && visibility != 3) || section == 0) continue

      val start = symbols.getInt(offset)
      if (start !in 1 until strings.size || strings[start] != '_'.code.toByte()) continue
      val end = (start until minOf(start + maximumSymbolLength + 1, strings.size))
        .firstOrNull { strings[it].toInt() == 0 } ?: continue
      val name = String(strings, start, end - start, Charsets.US_ASCII)
      if (name in snapshotSymbols) found.add(name)
      if (snapshotSymbolPairs.any(found::containsAll)) return true
    }
    return false
  }

  private fun read(apk: ZipFile, library: ZipEntry, offset: Long, count: Long): ByteBuffer? {
    // Bound metadata buffers and keep library contents out of memory.
    if (count !in 1..MAX_TABLE_BYTES || offset < 0 || offset > library.size - count) return null
    return DataInputStream(apk.getInputStream(library)).use { input ->
      var remaining = offset
      while (remaining > 0) {
        val skipped = input.skip(remaining)
        if (skipped > 0) {
          remaining -= skipped
        } else {
          if (input.read() == -1) return null
          remaining--
        }
      }
      val bytes = ByteArray(count.toInt())
      input.readFully(bytes)
      ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    }
  }

  private fun ByteBuffer.word(offset: Int, is64Bit: Boolean): Long =
    if (is64Bit) getLong(offset) else getInt(offset).toLong() and 0xffff_ffffL

  private fun ByteBuffer.unsignedShort(offset: Int): Int = getShort(offset).toInt() and 0xffff
}
