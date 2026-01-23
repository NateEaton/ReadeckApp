# Functional Specification: Unread View as Default

**Document Version:** 1.0
**Author:** Nate Eaton
**Date:** 2026-01-22
**Target Application:** ReadeckApp
**Upstream Repository:** [jensomato/ReadeckApp](https://github.com/jensomato/ReadeckApp)
**Intended Release:** Next Minor Release

---

## 1. Executive Summary

**Problem Statement:**
When users open the ReadeckApp, they are shown all bookmarks regardless of read status. This differs from the Readeck web application's default view and forces users to manually filter to "Unread" on every app launch to see items they haven't read yet.

**Proposed Solution:**
Change the default filter state to show only unread bookmarks when the app first launches, matching the behavior of the Readeck web application.

**User Value:**
- Immediate focus on unread content without manual filtering
- Consistent experience between mobile and web applications
- Reduces friction for the primary use case: finding something to read
- Aligns with the core purpose of Readeck as a "read it later" service

**Alignment with Readeck Philosophy:**
This feature achieves parity with the Readeck web application, which defaults to showing unread items. The web behavior reflects the product's primary use case: users save articles to read later, and naturally want to see what they haven't read yet when they open the app.

---

## 2. User Stories

### Primary User Story
**As a** Readeck user who has both read and unread bookmarks
**I want** the app to show only unread bookmarks by default when I open it
**So that** I can immediately see what I still need to read without having to manually filter

**Acceptance Criteria:**
- [ ] When the app first launches, the bookmark list displays only unread items
- [ ] The filter indicator/UI shows that "Unread" filter is active
- [ ] User can still access all bookmarks by changing the filter
- [ ] Filter selection persists within the current session
- [ ] Next app launch resets to Unread view (doesn't persist selected filter across sessions)

### Secondary User Stories

**As a** new user with no bookmarks yet
**I want** the empty state to be clear and helpful
**So that** I understand the app is working and showing unread items (of which there are none)

**Acceptance Criteria:**
- [ ] Empty state message is contextually appropriate for "unread" filter
- [ ] No confusing error messages or blank screens

---

## 3. Functional Requirements

### 3.1 Core Functionality

| Req ID | Requirement | Priority | Notes |
|--------|-------------|----------|-------|
| FR-01 | Default filter state must be "Unread" on app launch | Must Have | Core functionality |
| FR-02 | Filter UI must visually indicate "Unread" is active | Must Have | User awareness |
| FR-03 | User can change filter to All/Archived/Favorites/etc. | Must Have | Existing functionality must work |
| FR-04 | Filter changes persist within current session | Should Have | Standard UX pattern |
| FR-05 | Filter resets to Unread on next app launch | Must Have | Matches web behavior |

### 3.2 User Interface Requirements

**Entry Points:**
- No new entry points; this changes the default state of existing bookmark list view

**UI Components Required:**
- No new UI components needed
- Existing filter chips/buttons will show "Unread" as selected by default

**User Interactions:**
1. User opens ReadeckApp
2. App loads bookmark list with Unread filter active
3. List displays only bookmarks with unread status
4. Filter UI shows "Unread" as selected (visual indicator)
5. User can tap other filter options to change view

**Visual Design Notes:**
- Material Design 3 compliance: Uses existing filter chip design
- Accessibility considerations: No changes to existing accessibility support
- Dark mode support: No changes needed (uses existing theming)

### 3.3 Data Requirements

**Data Input:**
- None (this is a default state change)

**Data Storage:**
- No new data storage required
- No database schema changes
- Filter state exists only in ViewModel memory (session-scoped)

**Data Output:**
- Bookmark list filtered to show only items where `isUnread == true`

**API Integration:**
- No API changes required
- Uses existing bookmark data with existing `isUnread` field

### 3.4 Business Logic

**Rules and Constraints:**
- Filter state is set to "Unread" when ViewModel is initialized
- Standard filter logic applies (no special cases)

**Edge Cases:**
- **Empty states:**
  - User has no bookmarks: Shows "No bookmarks" message
  - User has bookmarks but all are read: Shows "No unread bookmarks" message (existing empty state for filtered view)
  - User has unread bookmarks: Shows normal list
- **Offline mode:** No impact; filter works with locally cached data
- **Slow network:** No impact; filter is client-side only
- **Large datasets:** No performance impact; filter is applied at database query level (existing functionality)
- **Concurrent modifications:** No new concurrency issues; uses existing bookmark sync

---

## 4. Non-Functional Requirements

### 4.1 Performance
- No performance impact; uses existing filter query logic
- Database query performance unchanged (Room query optimization already in place)

### 4.2 Usability
- Immediately intuitive: Most users expect to see unread items by default in a "read later" app
- Consistent with web application behavior
- No learning curve; reduces friction instead

### 4.3 Reliability
- No new error conditions
- No special error handling needed
- Uses existing, proven filter logic

### 4.4 Security
- No security implications
- No user data exposure changes

### 4.5 Compatibility
- **Minimum Android Version:** API 24 / Android 7.0 (matches app minimum)
- **Target Android Version:** Current target SDK
- **Device Types:** All supported devices (phone, tablet, foldable)
- **Screen Sizes:** No responsive design changes needed

---

## 5. User Experience (UX) Flows

### 5.1 Happy Path
```
1. User opens ReadeckApp (fresh launch)
2. App initializes BookmarkListViewModel
3. ViewModel sets initial FilterState with unread=true
4. Database query returns only unread bookmarks
5. UI displays filtered list with "Unread" filter visually selected
6. User sees their unread bookmarks immediately
```

### 5.2 Alternative Flows

**User wants to see all bookmarks:**
1. User opens app (sees unread by default)
2. User taps "All" filter chip
3. Filter state updates to show all bookmarks
4. List refreshes to show all items
5. Filter selection persists while app is open
6. On next app launch, resets to Unread

**User has no unread bookmarks:**
1. User opens app
2. App applies Unread filter
3. Query returns empty result set
4. UI shows empty state: "No unread bookmarks"
5. User can tap "All" to see their read bookmarks

**Cancellation Flow:**
- N/A (no multi-step operation to cancel)

**Undo/Redo:**
- N/A (filter changes are non-destructive and immediately reversible by selecting different filter)

---

## 6. Visual Specifications

### 6.1 UI Mockups

**Current Behavior (Before):**
```
TopAppBar: "Bookmarks"
Filter Row: [All*] [Unread] [Archived] [Favorites]
             ^^^^ selected by default
List: Shows all bookmarks (read and unread)
```

**New Behavior (After):**
```
TopAppBar: "Bookmarks"
Filter Row: [All] [Unread*] [Archived] [Favorites]
                   ^^^^^^^ selected by default
List: Shows only unread bookmarks
```

*Visual indicator (underline, background color, or checkmark) shows selected filter

### 6.2 Copy (Text Content)

No new strings required. Existing strings cover all states:
- Filter labels: Already exist ("All", "Unread", "Archived", "Favorites")
- Empty state: Existing string for "No unread bookmarks" or similar

---

## 7. Technical Constraints

### 7.1 Architecture Alignment
- **Pattern:** MVVM with Jetpack Compose
- **State Management:** ViewModel with StateFlow (existing `FilterState`)
- **Navigation:** No changes to navigation
- **Dependency Injection:** No new dependencies
- **Database:** Uses existing Room queries

### 7.2 Existing Code Impact

**Files Expected to Change:**
- `app/src/main/java/de/readeckapp/ui/list/BookmarkListViewModel.kt`
  - Change initial value of `_filterState` MutableStateFlow
  - Minimal change: Single line modification

**File:** `app/src/test/java/de/readeckapp/ui/list/BookmarkListViewModelTest.kt`
  - Update unit tests to expect unread as default state
  - Tests that verify initial filter state need updating

**New Files Expected:**
- None

**Backward Compatibility:**
- No data migration required
- No API version compatibility issues
- Users' existing bookmarks are unaffected

### 7.3 Testing Requirements

**Unit Tests:**
- [x] ViewModel initializes with FilterState(unread=true)
- [x] Filter state can be changed to All/Archived/Favorites
- [x] Bookmark flow emits only unread items with default filter
- [x] Empty state logic works correctly with unread filter

**Integration Tests:**
- [x] UI displays unread bookmarks on initial load
- [x] Filter chip shows "Unread" as selected
- [x] Changing filter updates displayed bookmarks

**Manual Testing Scenarios:**
- [x] Fresh app install, add bookmarks, verify unread shown by default
- [x] App with mix of read/unread bookmarks
- [x] App with all read bookmarks (empty state)
- [x] App with no bookmarks (empty state)
- [x] Filter change persists during session
- [x] Filter resets to Unread on app restart

---

## 8. Dependencies

### 8.1 Internal Dependencies
- Depends on existing FilterState implementation
- Depends on existing bookmark DAO queries for unread items
- No blocking dependencies; feature is additive

### 8.2 External Dependencies
- No new library dependencies
- No build.gradle.kts changes needed

### 8.3 API Dependencies
- No server API changes required
- Works with all current Readeck server versions
- `isUnread` field already exists in bookmark data model

---

## 9. Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation Strategy |
|------|------------|--------|---------------------|
| Users confused by not seeing all bookmarks on launch | Low | Low | Filter UI clearly shows "Unread" is selected; users familiar with web app expect this |
| Users with all-read bookmarks see empty state | Low | Low | Empty state message is clear; "All" filter is one tap away |
| Unit tests need updates | High | Low | Update tests as part of implementation; straightforward changes |
| Conflicts with upstream changes to FilterState | Low | Medium | Code change is minimal (one line); easy to rebase if needed |

---

## 10. Success Criteria

**Definition of Done:**
- [x] FilterState initializes with `unread=true` in BookmarkListViewModel
- [x] All existing unit tests updated to reflect new default
- [x] New unit test verifies default filter state
- [x] Manual testing confirms unread bookmarks shown by default
- [x] No regressions in filter functionality
- [x] Code follows existing patterns and style
- [x] Changes reviewed by AI code quality validator

**User Acceptance:**
- [x] App opens to unread view matching web application
- [x] Intuitive and non-disruptive change
- [x] All filter options still accessible and functional
- [x] Positive experience in personal testing

**Code Quality:**
- [x] Minimal, focused change (1-line change + test updates)
- [x] No security concerns introduced
- [x] No performance impact
- [x] Clean git history with clear commit message

---

## 11. Future Enhancements (Out of Scope)

**Persistent Filter Preference:**
- Allow user to set their preferred default filter in Settings
- Store preference and apply on app launch
- **Rationale for deferring:** Adds complexity; web app doesn't offer this; current change achieves parity

**Filter State Preservation Across Launches:**
- Remember user's last selected filter and restore it
- **Rationale for deferring:** Conflicts with "reset to unread" philosophy; better as separate feature discussion with maintainer

---

## 12. References

**Related Issues/Discussions:**
- None currently; this is proactive parity improvement

**Design References:**
- Readeck web application default view behavior
- Material Design 3 - Filter chips: https://m3.material.io/components/chips/guidelines

**Technical Documentation:**
- Jetpack Compose StateFlow documentation
- Room database queries (existing implementation)

---

## 13. Approval and Sign-Off

**Specification Author:** Nate Eaton
**Date:** 2026-01-22

**Reviewed By:** [Pending technical spec review]
**Date:** [Pending]

**Implementation Approved:** [ ] Yes [ ] No [x] Pending Technical Review

---

## Appendix A: Glossary

| Term | Definition |
|------|------------|
| Bookmark | A saved article/webpage in Readeck |
| Unread | A bookmark that has not been marked as read by the user |
| Filter State | The currently selected filter criteria (All, Unread, Archived, Favorites, etc.) |
| Session | The period from app launch to app termination (filter state persists during session) |

---

## Appendix B: Change Log

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-22 | Nate Eaton | Initial specification |

---

## Appendix C: Implementation Notes (Technical Preview)

**Expected Code Change:**

**File:** `BookmarkListViewModel.kt` (line ~60)

```kotlin
// BEFORE
private val _filterState = MutableStateFlow(FilterState())

// AFTER
private val _filterState = MutableStateFlow(FilterState(unread = true))
```

**File:** `BookmarkListViewModelTest.kt`

Update test expectations:
```kotlin
// Tests that verify initial state should expect:
filterState.value.unread == true
// instead of default FilterState()
```

**Estimated Implementation Effort:**
- Code change: 1 line
- Test updates: 3-5 test methods
- Manual testing: 30 minutes
- Total effort: < 1 hour

**Risk Assessment:** Very Low
- Minimal code surface area
- Well-understood change
- Easy to verify
- Easy to revert if needed
