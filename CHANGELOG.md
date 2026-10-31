<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Analytics Viewer For Firebase Changelog

## [Unreleased]

## [0.4.1]

### Fixed
- Event-name search works independently of parameter filters, including while analytics collection is paused.

## [0.4.0]

### Added
- Parameter filters dialog with per-parameter visibility controls and optional value matching.
- Pause and resume controls for incoming analytics events; events received while paused appear on resume.

### Changed
- Events match the selected parameter filters when at least one selected parameter is present; all filled-in values must match.
- Applying parameter filters resyncs before filtering, and resync preserves filter settings.
- The Parameters tab displays only selected parameters when filters are selected.
- Parameter filters button appears disabled when no analytics events are available.

## [0.2.0]

### Changed
- Internal restructuring: the tool window was split into small components (`model`, `data`, `ui`) with no visual or behavioral changes.

## [0.1.1]

### Added
- ADB status indicator (green: device connected, red: no device / adb not available).
- Automatic resync when a device reconnects.

### Fixed
- Resync duplicated events; it now clears events and user properties and reloads them from logcat.

## [0.1.0]

### Added
- Always-active filter field below the toolbar (replaces the search button), with debounce.
- Icon-only Sync and Clear actions with hover feedback.
- Strings moved to `strings.xml`.

### Changed
- New button-style event list, folder-style Parameters/Properties tabs with fading side lines.
- Thinner, easier-to-grab resize divider.
- More spacing between parameter names and values.

### Fixed
- IDE freeze when clearing the filter with many accumulated events.

## [0.0.3]

### Added
- Search button to filter events by name.

### Fixed
- Clear no longer removes user properties.

## [0.0.2]

### Changed
- Long event titles are truncated with an ellipsis; full title shown in a tooltip.

## [0.0.1]

### Added
- Real-time list of Firebase Analytics events read from logcat through adb.
- Parameters and user properties views.
- Clear and Sync buttons.
