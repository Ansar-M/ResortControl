# Implementation Plan - Add Date Availability Filter

This plan describes how to add a date range availability filter to the `CabinListScreen`, allowing users to find properties that are free during specific dates.

## User Review Required

> [!IMPORTANT]
> - The date range selection will use the same `DateRangePicker` component as the reservation creation screen to ensure a consistent "mini window" experience with both text input and calendar selection.
> - When a date filter is active, only cabins with NO overlapping bookings during that period will be shown.

## Proposed Changes

### Resources

#### [MODIFY] [strings.xml](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/src/main/res/values/strings.xml)
- Add `filter_dates_label`: "Check availability"
- Add `select_dates_title`: "Select Dates"
- Add `all_dates_label`: "All dates"

#### [MODIFY] [strings.xml (ru)](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/src/main/res/values-ru/strings.xml)
- Add `filter_dates_label`: "Проверить доступность"
- Add `select_dates_title`: "Выберите даты"
- Add `all_dates_label`: "Любые даты"

### UI Components

#### [MODIFY] [CabinListScreen.kt](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/src/main/java/com/example/resortcontrol/CabinListScreen.kt)
- **New State**:
    - `filterStartDate` and `filterEndDate` (using `rememberSaveable`) to store the selected range.
    - `showDatePicker` state to control the visibility of the calendar dialog.
- **Filtering Logic**:
    - Update `filteredCabins` to check for date overlaps. If dates are selected, a cabin is included only if `cabin.bookings` contains NO bookings where `(selectedStart < bookingEnd) AND (selectedEnd > bookingStart)`.
- **Filter Dialog Update**:
    - Add a row or button titled "Check availability" (localized).
    - Display the selected dates or "All dates" if none are selected.
    - Clicking this will open a secondary `AlertDialog` containing the `DateRangePicker`.
- **Date Picker Dialog**:
    - Implement a `DateRangePicker` dialog similar to the one in `BookingCalendar.kt`.
    - Include a "Clear" or "Reset" option within this picker to remove the date filter.

## Verification Plan

### Manual Verification
- **Functional Testing**:
    - Open Filters, click "Check availability".
    - Select a date range where you know some cabins are booked.
    - Verify that booked cabins disappear from the list.
    - Change dates and verify the list updates correctly.
    - Reset the date filter and verify all cabins return.
- **UI/UX Consistency**:
    - Verify the calendar matches the reservation calendar's look and feel.
    - Verify localization in both English and Russian.
