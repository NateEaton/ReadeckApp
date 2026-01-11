# Upstream Contribution Analysis

## Overview

This document analyzes all changes made to this fork of [ReadeckApp](https://github.com/jensomato/ReadeckApp) since forking, with the goal of identifying discrete features suitable for contributing back to the upstream repository via separate pull requests.

**Fork Base:** v0.8.0 (commit `246e719`)
**Total New Commits:** 51
**Upstream Status:** No new commits since fork (as of analysis date)

---

## Table of Contents

1. [Feature Inventory](#feature-inventory)
2. [Exclusions (Not for Upstream)](#exclusions-not-for-upstream)
3. [Recommended PR Sequence](#recommended-pr-sequence)
4. [Detailed Feature Analysis](#detailed-feature-analysis)
5. [Dependency Map](#dependency-map)
6. [Entanglement Issues](#entanglement-issues)
7. [Implementation Plan](#implementation-plan)

---

## Feature Inventory

| # | Feature | Commits | Complexity | Dependencies | Upstream Priority |
|---|---------|---------|------------|--------------|-------------------|
| 1 | Search Functionality | 1 + 4 fixes | Medium | None | **HIGH** |
| 2 | Unread View as Default | 1 | Low | None | **HIGH** |
| 3 | Delete with Undo (SnackBar) | 1 | Low | None | **MEDIUM** |
| 4 | Filter Name in Header | 3 | Low | None | **MEDIUM** |
| 5 | Labels Management System | 26 | High | Search (optional) | **HIGH** |
| 6 | Details Modal Dialog | 12 | Medium | Labels | **MEDIUM** |

---

## Exclusions (Not for Upstream)

The following changes should **NOT** be submitted to upstream:

### 1. 3-Dot Menu Replacement with Action Icons
- **Commit:** `84491f7`
- **Reason:** Opinionated UX change that removes existing functionality (Mark as Read, Share Link)
- **Files:** `BookmarkCard.kt` only
- **Entanglement:** Low (isolated to single file)

### 2. GitHub Actions / CI-CD
- **Commits:** `a3648f1`, `a1dce49`, `4777dd1`, `a78ae64`
- **Reason:** Fork-specific CI/CD infrastructure
- **Files:** `.github/workflows/*`, `scripts/check-kotlin-syntax.py`
- **Entanglement:** Low (isolated files)

### 3. Debug Signing Configuration
- **Commits:** `728bbd6`, `c9e97f6`, `389f7e1`
- **Reason:** Fork-specific build configuration for CI compatibility
- **Files:** `app/build.gradle.kts`
- **Entanglement:** **HIGH** - mixed with app configuration

### 4. Local Development Materials
- **Files:** `_notes/DEVELOPMENT_SETUP.md`, `_notes/ReadeckAppLog.txt`
- **Commit:** `51a3bd6` (entangled with search feature)
- **Entanglement:** **MEDIUM** - `_notes/` bundled with search commit

### 5. Version Revert
- **Commit:** `26beb3c`
- **Reason:** Fork-specific version management
- **Files:** `app/build.gradle.kts`

---

## Recommended PR Sequence

Based on dependencies, complexity, and your stated priorities:

### PR 1: Search Functionality
**Priority:** Highest (already partially on develop)

| Aspect | Details |
|--------|---------|
| **Commits to include** | `51a3bd6` (search feature) |
| **Commits to exclude** | Test timing fixes that may be CI-specific |
| **Files** | BookmarkDao.kt, BookmarkRepository.kt, BookmarkRepositoryImpl.kt, BookmarkListViewModel.kt, BookmarkListScreen.kt, strings.xml |
| **Cleanup required** | Remove `_notes/DEVELOPMENT_SETUP.md` from commit |
| **Risk** | Low - self-contained feature |

### PR 2: Unread View as Default
**Priority:** High (simple, matches Readeck web behavior)

| Aspect | Details |
|--------|---------|
| **Commits to include** | `61174f5` |
| **Files** | BookmarkListViewModel.kt (1 line), BookmarkListViewModelTest.kt (test updates) |
| **Cleanup required** | None |
| **Risk** | Very Low - single line change |

### PR 3: Delete with Undo (SnackBar)
**Priority:** Medium

| Aspect | Details |
|--------|---------|
| **Commits to include** | `b866c7b` |
| **Files** | BookmarkDetailScreen.kt, BookmarkDetailViewModel.kt, BookmarkListScreen.kt, BookmarkListViewModel.kt |
| **Cleanup required** | None |
| **Risk** | Low - standard Material Design pattern |

### PR 4: Filter Display Improvements
**Priority:** Medium

| Aspect | Details |
|--------|---------|
| **Commits to include** | `cbe6530`, `63c254d`, `a5a5359` (squash recommended) |
| **Files** | BookmarkListScreen.kt, BookmarkListViewModel.kt, strings.xml, BookmarkListViewModelTest.kt |
| **Cleanup required** | None |
| **Risk** | Low - UI polish with tests |

### PR 5: Labels Management System
**Priority:** High (but complex, submit last)

| Aspect | Details |
|--------|---------|
| **Core commits** | `36ec7bc`, `0032bad`, `3888496`, `46b0519`, `dcde2d4` |
| **Fix commits** | 14+ commits fixing label functionality |
| **New files** | `LabelsDialog.kt` |
| **Modified files** | BookmarkDao.kt, BookmarkRepository.kt, BookmarkRepositoryImpl.kt, BookmarkListViewModel.kt, BookmarkListScreen.kt, BookmarkDetailsDialog.kt, strings.xml, and more |
| **Cleanup required** | Significant - must untangle from excluded features |
| **Risk** | Medium-High - large surface area |

**Note:** The Details Modal (`BookmarkDetailsDialog.kt`) is tightly coupled to labels management and should be included in the Labels PR rather than submitted separately.

---

## Detailed Feature Analysis

### Feature 1: Search Functionality

**Commit:** `51a3bd6` - feat: add search functionality for bookmarks

**Architecture:**
```
UI Layer (BookmarkListScreen)
    ↓
ViewModel (BookmarkListViewModel) - 300ms debounce
    ↓
Repository (BookmarkRepositoryImpl)
    ↓
DAO (BookmarkDao) - LIKE query with COLLATE NOCASE
    ↓
SQLite Database
```

**Key Implementation Details:**
- Case-insensitive search on `title`, `labels`, and `siteName`
- Search is additive to existing filters (can search within Favorites, etc.)
- 300ms debounce prevents excessive database queries
- Distinct empty state messages for search vs. filter results

**Files Changed:**
- `BookmarkDao.kt` - New `searchBookmarkListItems()` with dynamic SQL
- `BookmarkRepository.kt` - Interface method
- `BookmarkRepositoryImpl.kt` - Implementation
- `BookmarkListViewModel.kt` - State management (`_searchQuery`, `_isSearchActive`)
- `BookmarkListScreen.kt` - Search UI in TopAppBar
- `strings.xml` - 5 new strings

**Test Fixes (may or may not include):**
- `625fafc` - Skip debounce for empty queries (test timing)
- `f39fca5` - Use explicit delay for test compatibility
- `76acd55` - Use combine+flatMapLatest pattern
- `a78ae64` - Async flow observation fixes

---

### Feature 2: Unread View as Default

**Commit:** `61174f5` - feat: make Unread view the default instead of All

**Change:** Single line modification
```kotlin
// Before
private val _filterState = MutableStateFlow(FilterState())

// After
private val _filterState = MutableStateFlow(FilterState(unread = true))
```

**Rationale:** Matches the default behavior of the Readeck web application.

---

### Feature 3: Delete with Undo (SnackBar)

**Commit:** `b866c7b` - feat: implement delete confirmation with SnackBar undo for bookmarks

**Implementation Pattern:**
1. User clicks delete
2. UI shows "Bookmark deleted" SnackBar with UNDO action
3. Background coroutine waits 5 seconds
4. If UNDO clicked, `pendingDeleteJob.cancel()` prevents deletion
5. If timeout expires, actual deletion proceeds

**Key Code:**
```kotlin
pendingDeleteJob = viewModelScope.launch {
    delay(5000) // 5 second undo window
    updateBookmarkUseCase.deleteBookmark(bookmarkId)
}
```

---

### Feature 4: Filter Display Improvements

**Commits:**
- `cbe6530` - Show current filter name in header
- `63c254d` - Make type and status filters mutually exclusive
- `a5a5359` - Update tests for mutual exclusivity

**Behavior:**
- Header displays context-aware title: "Unread Bookmarks", "Archived Bookmarks", etc.
- Selecting a type filter (Articles/Videos/Pictures) clears status filters
- Selecting a status filter (Unread/Archived/Favorites) clears type filter
- Prevents confusing UI state where multiple categories appear selected

---

### Feature 5: Labels Management System

**Commits:** 26 total (4 feature, 14 fix, 2 refactor, others)

**Capabilities Implemented:**
1. View all labels with bookmark counts (badge in nav drawer)
2. Filter bookmarks by label
3. Add/remove labels on existing bookmarks (Details dialog)
4. Add labels when creating new bookmarks
5. Rename labels (cascades to all bookmarks)
6. Delete labels with undo support
7. Comma-separated input handling
8. Full page view (not modal) for labels list

**New File:** `app/src/main/java/de/readeckapp/ui/list/LabelsDialog.kt` (101 lines)

**Architecture Additions:**
- `BookmarkDao` - Label queries, `observeAllLabelsWithCounts()`
- `BookmarkRepository` - `renameLabel()`, `deleteLabel()`, `updateLabels()`
- `BookmarkListViewModel` - Label state, view state for labels page

**API Integration:**
- Delta-based label updates (add/remove vs. full replace)
- Server-side label operations (rename cascades via API)

---

### Feature 6: Details Modal Dialog

**File:** `app/src/main/java/de/readeckapp/ui/detail/BookmarkDetailsDialog.kt` (262 lines)

**Displays:**
- Bookmark type (Article/Picture/Video)
- Language (if available)
- Word count
- Reading time
- Authors
- Description
- Labels (with inline editing)

**Note:** This feature is tightly coupled to labels management - labels can be added/removed directly from this dialog with immediate save.

---

## Dependency Map

```
                    ┌─────────────────────┐
                    │  Upstream v0.8.0    │
                    │    (Fork Base)      │
                    └──────────┬──────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
   │   Search     │    │   Unread     │    │   Delete     │
   │   (PR 1)     │    │   Default    │    │   with Undo  │
   │              │    │   (PR 2)     │    │   (PR 3)     │
   └──────────────┘    └──────────────┘    └──────────────┘
          │                    │                    │
          │                    │                    │
          ▼                    │                    │
   ┌──────────────┐            │                    │
   │   Filter     │◄───────────┘                    │
   │   Display    │                                 │
   │   (PR 4)     │                                 │
   └──────────────┘                                 │
          │                                         │
          │              ┌─────────────────────────┘
          ▼              ▼
   ┌─────────────────────────────┐
   │      Labels System          │
   │         (PR 5)              │
   │  ┌───────────────────────┐  │
   │  │   Details Modal       │  │
   │  │   (bundled w/labels)  │  │
   │  └───────────────────────┘  │
   └─────────────────────────────┘
```

**Key Dependencies:**
- **Search → Labels:** Search functionality searches label text; however, this is a soft dependency - search works without labels feature
- **Labels → Details Modal:** Details modal is the primary UI for per-bookmark label editing; must be bundled together
- **Filter Display → Unread Default:** These are independent but complementary; filter display can be submitted after unread default

---

## Entanglement Issues

### Issue 1: Search Commit Includes _notes/

**Commit:** `51a3bd6`
**Problem:** The search feature commit also added `_notes/DEVELOPMENT_SETUP.md`
**Solution:** Cherry-pick and amend to remove the `_notes/` file

```bash
# When creating PR branch:
git cherry-pick 51a3bd6
git reset HEAD~1 --soft
git restore --staged _notes/
git checkout -- _notes/
git commit -m "feat: add search functionality for bookmarks"
```

### Issue 2: Signing Config in build.gradle.kts

**Commits:** `728bbd6`, `c9e97f6`, `389f7e1`
**Problem:** Debug signing config changes mixed with app-level build file
**Solution:** When preparing any PR that touches `build.gradle.kts`, manually review and exclude signing changes

**Signing code to exclude:**
```kotlin
signingConfigs {
    // All content in this block is fork-specific
}
buildTypes {
    getByName("debug") {
        signingConfig = signingConfigs.getByName("debug")
        // This line is fork-specific
    }
}
```

### Issue 3: 3-Dot Menu Removal Affects BookmarkCard.kt

**Commit:** `84491f7`
**Problem:** If any upstream PR needs to modify `BookmarkCard.kt`, the excluded 3-dot menu changes may conflict
**Solution:**
- For upstream branches, start fresh from upstream/main
- Carefully apply only the relevant changes to `BookmarkCard.kt`
- The labels display (showing labels on cards) may need to be re-implemented without removing the 3-dot menu

---

## Implementation Plan

### Phase 1: Prepare Base Branches

```bash
# Ensure upstream remote is configured
git remote add upstream https://github.com/jensomato/ReadeckApp.git
git fetch upstream

# Create a clean working branch from upstream
git checkout -b upstream-contributions upstream/main
```

### Phase 2: Create PR Branches

#### PR 1 Branch: Search Feature
```bash
git checkout -b feature/search-functionality upstream/main

# Cherry-pick search commit
git cherry-pick 51a3bd6

# Remove _notes/ from the commit
git reset HEAD~1 --soft
git restore --staged _notes/
rm -rf _notes/  # or git checkout -- _notes/ if it existed before
git commit -m "feat: add search functionality for bookmarks

Add search capability to the bookmark list with the following features:
- Case-insensitive search on title, labels, and site name
- 300ms debounce to prevent excessive queries
- Search works additively with existing filters
- Distinct empty state messages for search vs filter results"

# Optionally include test timing fixes if they don't reference CI
git cherry-pick 625fafc  # Skip debounce for empty queries
# Review each fix commit to decide inclusion
```

#### PR 2 Branch: Unread Default
```bash
git checkout -b feature/unread-default upstream/main
git cherry-pick 61174f5
# This commit is clean - no changes needed
```

#### PR 3 Branch: Delete with Undo
```bash
git checkout -b feature/delete-undo upstream/main
git cherry-pick b866c7b
# This commit is clean - no changes needed
```

#### PR 4 Branch: Filter Display
```bash
git checkout -b feature/filter-display upstream/main

# Option A: Individual commits
git cherry-pick cbe6530
git cherry-pick 63c254d
git cherry-pick a5a5359

# Option B: Squash into single commit
git cherry-pick --no-commit cbe6530
git cherry-pick --no-commit 63c254d
git cherry-pick --no-commit a5a5359
git commit -m "feat: show current filter in header with mutual exclusivity

- Display active filter name in TopAppBar (Unread Bookmarks, etc.)
- Make type filters and status filters mutually exclusive
- Update tests to reflect new behavior"
```

#### PR 5 Branch: Labels System (Complex)
```bash
git checkout -b feature/labels-management upstream/main

# This requires careful cherry-picking due to many commits
# Start with core feature commits in order:

git cherry-pick 36ec7bc  # Initial labels menu
# Fix any conflicts, ensure no excluded content

git cherry-pick 0032bad  # Comprehensive label management
git cherry-pick 3888496  # Convert to full page view
git cherry-pick 46b0519  # UI polish
git cherry-pick dcde2d4  # Labels in create dialog

# Then apply necessary fixes in order:
git cherry-pick 92b7390  # Dialog rendering fix
git cherry-pick 403ba7c  # API calls fix
# ... continue with relevant fix commits

# Include Details Modal (tightly coupled):
git cherry-pick b5c4f60  # Initial details modal
git cherry-pick f8bcf8e  # Full label management in modal
git cherry-pick e472589  # Convert to full-screen
# ... and related fixes
```

### Alternative: Clean Room Reimplementation

For features with complex, tangled commit histories (especially **Labels** and potentially **Search**), a cleaner approach may be to start fresh from upstream and manually reimplement the feature using the current working code as a reference guide.

**When to use this approach:**
- Feature has many iterative fix commits (Labels has 26)
- Commits contain mixed changes (upstream + excluded content)
- Cherry-picking would require extensive conflict resolution
- You want a cleaner commit history for the upstream PR

**Benefits:**
- Produces clean, logical commits that are easier to review
- No risk of accidentally including excluded changes
- Opportunity to improve implementation while reimplementing
- Results in a proper "feature branch" workflow

#### Clean Room Process for Labels Feature

```bash
# 1. Start from clean upstream state
git checkout -b feature/labels-clean upstream/main

# 2. Keep your current main branch available for reference
#    (use a second terminal or IDE window to view the working code)

# 3. Implement in logical chunks, committing after each:

# Commit 1: Database layer
#   - Add label queries to BookmarkDao.kt
#   - Add observeAllLabelsWithCounts()
#   Reference: current BookmarkDao.kt lines with label functions

# Commit 2: Repository layer
#   - Add getAllLabelsWithCounts() to BookmarkRepository interface
#   - Implement in BookmarkRepositoryImpl
#   - Add renameLabel(), deleteLabel(), updateLabels()
#   Reference: current repository files

# Commit 3: ViewModel state management
#   - Add label-related state to BookmarkListViewModel
#   - Add label filtering logic
#   - Add CRUD operation handlers
#   Reference: current BookmarkListViewModel.kt

# Commit 4: Labels list UI
#   - Create LabelsDialog.kt (can copy directly - it's a new file)
#   - Add LabelsListView composable to BookmarkListScreen
#   - Wire up navigation drawer entry
#   Reference: current UI files

# Commit 5: Details modal with label editing
#   - Create BookmarkDetailsDialog.kt (new file)
#   - Add inline label editing capability
#   Reference: current BookmarkDetailsDialog.kt

# Commit 6: Labels in create bookmark flow
#   - Add labels parameter to bookmark creation
#   - Update create dialog UI
#   Reference: current implementation

# Commit 7: String resources
#   - Add all label-related strings to strings.xml
```

#### Clean Room Process for Search Feature

```bash
git checkout -b feature/search-clean upstream/main

# Commit 1: Database layer
#   - Add searchBookmarkListItems() to BookmarkDao.kt
#   Reference: lines 45-80 of current BookmarkDao.kt

# Commit 2: Repository layer
#   - Add interface method to BookmarkRepository.kt
#   - Implement in BookmarkRepositoryImpl.kt
#   Reference: current repository files

# Commit 3: ViewModel + UI
#   - Add search state management to BookmarkListViewModel.kt
#   - Add search UI to BookmarkListScreen.kt TopAppBar
#   - Add string resources
#   Reference: current ViewModel and Screen files
```

**Recommended approach by feature:**

| Feature | Recommended Approach | Reason |
|---------|---------------------|--------|
| Search | Cherry-pick with cleanup | Only 1 commit + test fixes, manageable |
| Unread Default | Cherry-pick | Single clean commit |
| Delete with Undo | Cherry-pick | Single clean commit |
| Filter Display | Cherry-pick + squash | 3 related commits, no entanglement |
| **Labels System** | **Clean room** | 26 commits, heavy entanglement, complex |

### Phase 3: Validation Checklist

For each PR branch, verify:

- [ ] No `_notes/` files included
- [ ] No `.github/workflows/` files included
- [ ] No `scripts/` directory additions
- [ ] No signing config changes in `build.gradle.kts`
- [ ] No version changes in `build.gradle.kts`
- [ ] `BookmarkCard.kt` retains 3-dot menu (if file is touched)
- [ ] All tests pass: `./gradlew test`
- [ ] App builds successfully: `./gradlew assembleDebug`
- [ ] Feature works as expected when tested on device/emulator

### Phase 4: Submit PRs

Recommended submission order:

1. **Search** - Most straightforward, already on develop
2. **Unread Default** - Trivial change, easy win
3. **Delete with Undo** - Standard UX improvement
4. **Filter Display** - Polish feature
5. **Labels System** - Submit last due to complexity

**Spacing:** Consider waiting for each PR to be reviewed/merged before submitting the next to avoid overwhelming the maintainer.

---

## Appendix: Complete Commit Reference

### Commits Suitable for Upstream

| Commit | Feature | PR # |
|--------|---------|------|
| `51a3bd6` | Search functionality | 1 |
| `625fafc` | Search: empty query fix | 1 |
| `f39fca5` | Search: test timing fix | 1 |
| `76acd55` | Search: flow pattern fix | 1 |
| `a78ae64` | Search: async test fix | 1 |
| `61174f5` | Unread as default | 2 |
| `b866c7b` | Delete with undo | 3 |
| `cbe6530` | Filter name in header | 4 |
| `63c254d` | Mutually exclusive filters | 4 |
| `a5a5359` | Filter test update | 4 |
| `36ec7bc` | Labels menu initial | 5 |
| `0032bad` | Labels CRUD | 5 |
| `3888496` | Labels full page | 5 |
| `46b0519` | Labels UI polish | 5 |
| `dcde2d4` | Labels in create dialog | 5 |
| `b5c4f60` | Details modal initial | 5 |
| `f8bcf8e` | Details modal labels | 5 |
| `e472589` | Details full-screen | 5 |
| (+ fix commits) | Various label fixes | 5 |

### Commits NOT for Upstream

| Commit | Reason |
|--------|--------|
| `84491f7` | 3-dot menu replacement |
| `0eca30a` | Related to 3-dot menu |
| `a3648f1` | GitHub Actions |
| `a1dce49` | Kotlin syntax checker |
| `4777dd1` | CI fix |
| `a78ae64` | CI trigger config |
| `728bbd6` | Debug signing |
| `c9e97f6` | Signing config |
| `389f7e1` | Signing config |
| `26beb3c` | Version revert |
| `f6ce5f8` | Log file upload |

---

## Notes

- The upstream repository has had no new commits since this fork was created, which simplifies rebasing/cherry-picking
- All feature implementations follow the existing architecture patterns in the codebase
- Test coverage has been maintained for modified functionality
- String resources follow the existing naming conventions

**Last Updated:** Analysis generated based on repository state at commit `8e173a0`
