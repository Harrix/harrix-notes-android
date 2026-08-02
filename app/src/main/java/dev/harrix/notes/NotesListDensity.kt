package dev.harrix.notes

/**
 * Density levels shared by the folder list, drawer tree, and pinned bar.
 * Each surface has its own preference; metrics below are applied per surface.
 *
 * Tree Compact is intentionally tighter than list Compact so the drawer
 * can show more of the hierarchy.
 */
enum class NotesListDensity(
    val verticalPaddingDp: Int,
    val iconSizeDp: Int,
    val mergedButtonHeightDp: Int,
    val treeVerticalPaddingDp: Int,
    val treeIconSizeDp: Int,
    val treeMergedButtonHeightDp: Int,
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
        treeVerticalPaddingDp = 0,
        treeIconSizeDp = 14,
        treeMergedButtonHeightDp = 20,
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
        treeVerticalPaddingDp = 2,
        treeIconSizeDp = 16,
        treeMergedButtonHeightDp = 26,
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
        treeVerticalPaddingDp = 6,
        treeIconSizeDp = 20,
        treeMergedButtonHeightDp = 32,
        pinnedIconSizeDp = 32,
        pinnedItemWidthDp = 84,
        pinnedLabelMinSp = 10f,
        pinnedLabelMaxSp = 13f,
        pinnedLabelHeightDp = 36,
        pinnedBarVerticalPaddingDp = 8,
    ),
    ;

    /** Fixed browse-list row height so folders and notes align. */
    val listRowHeightDp: Int
        get() = mergedButtonHeightDp + verticalPaddingDp * 2

    /** Fixed drawer-tree row height (Compact is much shorter than the list). */
    val treeRowHeightDp: Int
        get() = treeMergedButtonHeightDp + treeVerticalPaddingDp * 2

    companion object {
        val Default: NotesListDensity = Comfortable

        fun fromStorageKey(key: String?): NotesListDensity =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: Default
    }
}
