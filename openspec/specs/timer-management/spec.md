# timer-management Specification

## Purpose
Lets a user create, run and manage several countdown timers at once, each fully independent of the others, from a single list screen.

## Requirements

### Requirement: Independent timers
The app SHALL let the user keep any number of timers, and each timer SHALL start, pause, resume, reset, change and finish without affecting any other timer.

#### Scenario: Two timers run at different speeds of completion
- **WHEN** a 5-minute timer and a 10-minute timer are both running and the user pauses the 5-minute timer
- **THEN** the 10-minute timer keeps counting down and the 5-minute timer shows its remaining time as paused

#### Scenario: Newest timer first
- **WHEN** the user creates a timer
- **THEN** it appears at the top of the list

### Requirement: Accurate countdown
A running timer SHALL be defined by the moment it ends, so its remaining time stays correct regardless of whether the app is open, in the background, closed or the process was restarted.

#### Scenario: App reopened later
- **WHEN** a 10-minute timer is started, the app is closed, and the app is reopened 4 minutes later
- **THEN** the timer shows about 6 minutes remaining

### Requirement: Create a timer
The app SHALL provide a "New timer" panel with a label (optional, at most 40 characters), a duration (hours, minutes, seconds; minutes and seconds at most 59), a color from five choices and a sound, and SHALL start the timer immediately when the user confirms.

#### Scenario: Default values
- **WHEN** the user opens the New timer panel
- **THEN** the duration is 5 minutes, the label is empty, the sound is "App default" and a color is preselected

#### Scenario: Empty label
- **WHEN** the user starts a timer without a label
- **THEN** the timer is named "Timer N", where N is its sequence number

#### Scenario: Zero duration
- **WHEN** the user confirms a duration of zero
- **THEN** the timer is not created and the panel says "Set a duration longer than zero."

### Requirement: Quick start
The list screen SHALL offer quick-start chips for 1, 5, 10, 20 and 30 minutes and 1 hour; tapping one SHALL create and start a timer of that length, labelled with its duration.

#### Scenario: Quick 20 minutes
- **WHEN** the user taps "+ 20 min"
- **THEN** a running 20-minute timer labelled "20 min" appears at the top of the list

### Requirement: Timer card
Each timer SHALL be shown as a card with its color, label, remaining time, a progress ring with percentage, a status line, a full-screen button and a start/pause button, and the actions −1 min, +1 min, reset, edit and delete.

#### Scenario: Status line
- **WHEN** a timer is running, paused partway, ready (full duration) or finished
- **THEN** its status reads "Ends at <time>", "Paused · of <duration>", "Ready · <duration>" or "Time's up" respectively

#### Scenario: Own sound shown
- **WHEN** a timer has its own sound instead of the app default
- **THEN** the status line ends with " · <sound name>"

#### Scenario: Full-screen button
- **WHEN** the user taps a card's full-screen button
- **THEN** that timer opens full screen

### Requirement: Add and remove a minute
The +1 min action SHALL add one minute to a timer in any state, and the −1 min action SHALL remove one minute only while more than one minute remains.

#### Scenario: −1 min near the end
- **WHEN** a timer has one minute or less remaining, or has finished
- **THEN** the −1 min action is disabled

#### Scenario: +1 min on a finished timer
- **WHEN** the user adds a minute to a ringing timer
- **THEN** the ringing stops and the timer runs again for one minute

### Requirement: Edit a timer
The edit action SHALL open the timer panel prefilled with the timer's label, duration, color and sound. Saving a new label, color or sound SHALL apply in place without affecting the countdown. Saving a new duration SHALL start the timer over from the new duration: a running timer keeps running, any other timer becomes ready.

#### Scenario: Rename a running timer
- **WHEN** the user changes only the label of a running timer and saves
- **THEN** the label changes and the countdown continues unchanged, with no confirmation

#### Scenario: New duration on a timer with progress
- **WHEN** the user saves a new duration for a timer that is running, or paused with time already counted
- **THEN** a "Restart timer?" confirmation explains that the timer will stop and start again from the new duration, and the change applies only after the user confirms Restart

#### Scenario: New duration on a ready timer
- **WHEN** the user saves a new duration for a timer that has not started
- **THEN** the new duration applies without a confirmation

### Requirement: Delete with confirmation and undo
The delete action SHALL ask for confirmation, naming the timer and whether it is running or ringing, and after deletion SHALL offer Undo for a few seconds.

#### Scenario: Confirm and undo
- **WHEN** the user deletes "Pasta", confirms, then taps Undo
- **THEN** "Pasta" returns to its previous position in the list with its state preserved

#### Scenario: Undo after a running timer would have finished
- **WHEN** a running timer is deleted and restored after its end time has passed
- **THEN** it is restored as finished and rings

### Requirement: Pause all
The list screen SHALL provide a Pause all action, enabled only while at least one timer is running.

#### Scenario: Pause everything
- **WHEN** three timers are running and the user taps Pause all
- **THEN** all three timers are paused with their remaining time kept

### Requirement: Summary
The list screen SHALL show how many timers are running, the total, and how many are ringing when any are.

#### Scenario: Ringing count
- **WHEN** two timers run, one rings and four exist
- **THEN** the summary reads "2 running · 4 total · 1 ringing"

### Requirement: Persistence
Timers and their state SHALL survive closing the app, process death, app updates and phone restarts.

#### Scenario: Phone restart
- **WHEN** the phone restarts while a timer is running
- **THEN** after restart the timer is still listed and finishes at its original end time
