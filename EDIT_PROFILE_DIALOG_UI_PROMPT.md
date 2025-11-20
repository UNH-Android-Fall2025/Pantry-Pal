# Edit Profile Dialog UI Prompt

## Context
Create an Android XML layout file for a custom dialog/popup that allows users to edit their profile. This dialog should match the existing PantryPal app design system and theme.

## Design Requirements

### Layout Structure
- **Root Container**: LinearLayout (vertical orientation)
- **Background**: White (#FFFFFF)
- **Padding**: 24dp on all sides
- **Width**: Match parent (will be constrained by dialog)
- **Height**: Wrap content

### Dialog Title Section
- **Title Text**: "Edit Profile"
  - Text size: 20sp
  - Text style: bold
  - Text color: #1a1c1a
  - Layout margin bottom: 20dp

### Option Cards (Two Cards)

#### Card 1: Edit Username
- **Container**: CardView
  - Corner radius: 8dp
  - Elevation: 1dp
  - Background color: #FFFFFF
  - Margin bottom: 12dp
  - Clickable: true
  - Focusable: true
  - Foreground: selectableItemBackground (for ripple effect)

- **Content Layout**: LinearLayout (horizontal)
  - Padding: 16dp
  - Gravity: center_vertical

- **Icon Container**: LinearLayout
  - Size: 40dp x 40dp
  - Background: @drawable/primary_container_light (light green container)
  - Gravity: center
  - Icon: @drawable/ic_edit
  - Icon size: 24dp x 24dp
  - Icon tint: #0b210c (dark green)

- **Text**: "Edit Username"
  - Text size: 16sp
  - Text style: normal
  - Text color: #1a1c1a
  - Layout weight: 1
  - Margin start: 16dp

- **Chevron Icon**: ImageView
  - Icon: @drawable/ic_chevron_right
  - Size: 24dp x 24dp
  - Tint: #434843 (gray)

#### Card 2: Change Profile Picture
- **Container**: CardView (same styling as Card 1)
  - Margin bottom: 16dp (last card)

- **Content Layout**: LinearLayout (horizontal)
  - Same structure as Card 1

- **Icon Container**: LinearLayout
  - Same styling as Card 1
  - Icon: @drawable/ic_person (or ic_profile if available)
  - Same icon styling

- **Text**: "Change Profile Picture"
  - Same text styling as Card 1

- **Chevron Icon**: Same as Card 1

### Cancel Button
- **Type**: Button
- **Text**: "Cancel"
- **Text size**: 16sp
- **Text style**: normal
- **Text color**: #434843 (gray)
- **Background**: @drawable/rounded_button_bordered (bordered button style)
- **Padding**: 
  - Horizontal: 24dp
  - Vertical: 12dp
- **Width**: Match parent
- **Layout margin**: Top 16dp (optional, for spacing)

## Color Palette
- **Primary Green**: #4CAF50
- **Dark Green**: #0b210c
- **Light Green Background**: @drawable/primary_container_light
- **Primary Text**: #1a1c1a
- **Secondary Text**: #434843
- **Hint Text**: #6B7280
- **White Background**: #FFFFFF
- **Card Background**: #FFFFFF

## Typography
- **Title**: 20sp, bold, #1a1c1a
- **Option Text**: 16sp, normal, #1a1c1a
- **Button Text**: 16sp, normal, #434843

## Spacing & Layout
- **Dialog padding**: 24dp
- **Card padding**: 16dp
- **Card margin bottom**: 12dp (16dp for last card)
- **Icon container**: 40dp x 40dp
- **Icon size**: 24dp x 24dp
- **Text margin start**: 16dp (from icon)
- **Card corner radius**: 8dp
- **Card elevation**: 1dp

## Interactive Elements
- Cards should be clickable with ripple effect
- Use `android:clickable="true"` and `android:focusable="true"`
- Use `android:foreground="?attr/selectableItemBackground"` for ripple
- Cards should have proper touch feedback

## Drawable Resources Needed
- `@drawable/primary_container_light` - Light green rounded container for icons
- `@drawable/ic_edit` - Edit icon
- `@drawable/ic_person` or `@drawable/ic_profile` - Profile/person icon
- `@drawable/ic_chevron_right` - Right chevron icon
- `@drawable/rounded_button_bordered` - Bordered button style

## XML Structure Example
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp"
    android:background="#FFFFFF">
    
    <!-- Title -->
    <!-- Option Card 1: Edit Username -->
    <!-- Option Card 2: Change Profile Picture -->
    <!-- Cancel Button -->
</LinearLayout>
```

## Design Consistency
- Match the exact styling from activity_profile.xml cards
- Use the same icon container pattern (40dp square with light green background)
- Maintain consistent spacing and padding
- Use the same color scheme throughout
- Ensure cards have the same elevation and corner radius as profile page cards

## Additional Notes
- The dialog should feel like a natural extension of the profile page
- Cards should have a subtle shadow (elevation 1dp)
- Icons should be properly centered in their containers
- Text should be left-aligned with proper spacing from icons
- Chevron icons indicate that items are clickable
- The overall design should be clean, modern, and consistent with the app's minimalist aesthetic

