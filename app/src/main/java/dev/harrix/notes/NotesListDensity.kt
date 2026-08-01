package dev.harrix.notes

/**
 * Vertical density for folder/note rows in the Markdown Notes browser,
 * navigation tree, and pinned bottom bar.
 *
 * [verticalPaddingDp] is applied top and bottom of each list row.
 * [listRowHeightDp] keeps folder and note rows the same height.
 */
enum class NotesListDensity(
    val verticalPaddingDp: Int,
    val iconSizeDp: Int,
    val mergedButtonHeightDp: Int,
    val pinnedIconSizeDp: Int,
    val pinnedItemWidthDp: Int,
    val pinnedLabelMinSp: Float,
    val pinnedLabelMaxSp: Float,
    val pinnedLabelHeightDp: Int,
    val pinnedBarVerticalPaddingDp: Int,
) {
    Compact(
        verticalPaddingDp = 2,
        iconSizeDp = 18,
        mergedButtonHeightDp = 28,
        pinnedIconSizeDp = 20,
        pinnedItemWidthDp = 56,
        pinnedLabelMinSp = 8f,
        pinnedLabelMaxSp = 10f,
        pinnedLabelHeightDp = 24,
        pinnedBarVerticalPaddingDp = 4,
    ),
    Comfortable(
        verticalPaddingDp = 4,
        iconSizeDp = 20,
        mergedButtonHeightDp = 32,
        pinnedIconSizeDp = 28,
        pinnedItemWidthDp = 72,
        pinnedLabelMinSp = 9f,
        pinnedLabelMaxSp = 12f,
        pinnedLabelHeightDp = 32,
        pinnedBarVerticalPaddingDp = 6,
    ),
    Spacious(
        verticalPaddingDp = 10,
        iconSizeDp = 24,
        mergedButtonHeightDp = 40,
        pinnedIconSizeDp = 32,
        pinnedItemWidthDp = 84,
        pinnedLabelMinSp = 10f,
        pinnedLabelMaxSp = 13f,
        pinnedLabelHeightDp = 36,
        pinnedBarVerticalPaddingDp = 8,
    ),
    ;

    /** Fixed list-row height so folders and notes align. */
    val listRowHeightDp: Int
        get() = mergedButtonHeightDp + verticalPaddingDp * 2

    companion object {
        val Default: NotesListDensity = Comfortable

        fun fromStorageKey(key: String?): NotesListDensity =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: Default
    }
}
