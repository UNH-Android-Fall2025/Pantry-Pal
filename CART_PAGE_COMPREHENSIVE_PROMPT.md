# PantryPal Cart Page - Comprehensive UI Generation Prompt

## Prompt for Android XML Layout Generation: PantryPal Cart Page

Generate an Android XML layout for a CartActivity in the "PantryPal" app, adhering to Material 3 design principles. The layout should be fully responsive and optimized for mobile phones.

### Overall Design System & Colors:

- **Primary Accent Color**: `#4CAF50` (Primary Green) - for main actions, active states, and important text.
- **Background Color**: `#F8FAFC` (Light Grey/Off-White) - for the main screen background.
- **Card Background Color**: `#FFFFFF` (White) - for item cards and summary bar.
- **Dark Text Color**: `#121212` (Very Dark Grey) - for main titles and prominent text.
- **Medium Grey Text Color**: `#757575` (Medium Grey) - for subtitles, categories, and less prominent text.
- **App Bar Background Color**: `#f6f7f6` (Light Grey)
- **Divider Color**: `#BDBDBD` (Lighter Grey)
- **Delete Icon Tint**: `#FF5252` (Red) - for the trash/delete icon.

### Root Layout:

- Use `ConstraintLayout` as the root element.
- Set `android:fitsSystemWindows="true"` for proper full-screen handling.

### Layout Structure (Top-to-Bottom):

#### 1. Top App Bar (`topAppBar` - LinearLayout):
- Horizontal LinearLayout acting as a custom app bar.
- Background color: `#f6f7f6`.
- Padding: `16dp` horizontally and vertically.
- **Back Button** (`btnBack` - ImageView or ImageButton):
  - Dimensions: `48dp x 48dp`.
  - Icon: A standard Material Design back arrow icon.
  - Content Description: "Back button".
- **Cart Title** (`tvCartTitle` - TextView):
  - Centered horizontally within the app bar.
  - Text: "Cart".
  - Text Size: `20sp`.
  - Text Style: Bold.
  - Text Color: `#121212`.
  - `layout_weight="1"` to push other elements to sides.
- **Spacer** (View): To push the title to the center, similar to a layout_weight or a fixed-width empty view if needed.

#### 2. Main Scrollable Content (`scrollView` - ScrollView):
- Constrain to below `topAppBar` and above `bottomSummaryBar`.
- Contains the RecyclerView for cart items and the `emptyCartContainer`.
- The RecyclerView and `emptyCartContainer` should be mutually exclusive (one visible, one gone).

##### a. Cart Items List (`rvCartItems` - RecyclerView):
- Will contain multiple CardView items.
- Margins: `8dp` to `16dp` (horizontal).
- Spacing between items: `8dp` to `16dp` vertical spacing.
- Constrain to fill the ScrollView.

**Inner Layout for each Cart Item Card (CardView):**
- `CardView` with `8dp` corner radius and `2dp` elevation.
- Background color: `#FFFFFF`.
- Padding: `16dp` internally.
- Use `ConstraintLayout` inside the `CardView` for precise positioning.

- **Product Image/Icon** (ImageView):
  - Dimensions: `64dp` to `80dp` (square).
  - Left-aligned in the card.
  - Background: A light green drawable or color (e.g., a lighter shade of `#4CAF50`).
  - Content Description: "Product image".

- **Product Info** (LinearLayout - vertical orientation):
  - Constrain to the right of the product image, center vertically.
  - Flexible width (`wrap_content` or `0dp` with constraints).
  - **Product Name** (TextView):
    - Text Size: `16sp`.
    - Text Style: Bold.
    - Text Color: `#121212`.
  - **Category** (TextView):
    - Text Size: `12sp`.
    - Text Color: `#757575`.
  - **Price** (TextView):
    - Text Size: `14sp`.
    - Text Color: `#4CAF50`.

- **Trash Icon Button** (ImageView/ImageButton):
  - Dimensions: `24dp` to `32dp` (square).
  - Icon: A standard Material Design trash/delete icon.
  - Tint: `#FF5252` (Red).
  - Positioning: Constrain to the **top-right corner** of the `CardView` (top-to-top and end-to-end of parent).
  - Margins: `8dp` from top and right.
  - Content Description: "Delete item".

- **Quantity Controls** (LinearLayout - horizontal orientation):
  - Positioning: Constrain to the **bottom-right** of the `CardView` (bottom-to-bottom and end-to-end of parent).
  - Vertical spacing from Trash Icon: Minimum `8dp`.
  - Contains three elements:
    - **Minus Button** (Button/ImageButton): `40dp` square, icon: standard Material Design minus. Background: A subtle red or outlined. Text Color: `#121212`. Content Description: "Decrease quantity".
    - **Quantity Display** (TextView): `40dp` width, centered text. Text Size: `18sp`. Text Color: `#121212`. Placeholder text: "1".
    - **Plus Button** (Button/ImageButton): `40dp` square, icon: standard Material Design plus. Background: A subtle green or outlined. Text Color: `#121212`. Content Description: "Increase quantity".

##### b. Empty Cart State (`emptyCartContainer` - LinearLayout - vertical orientation):
- Should be `android:visibility="gone"` by default, visible when cart is empty.
- Centered vertically and horizontally within the ScrollView.
- **Cart Icon** (ImageView):
  - Dimensions: `96dp x 96dp`.
  - Icon: A standard Material Design shopping cart icon.
  - Tint: `#757575` with 50% alpha (or a lighter shade of gray).
  - Content Description: "Empty cart icon".
- **Title** (TextView):
  - Text: "Your cart is empty".
  - Text Size: `20sp`.
  - Text Color: `#121212`.
- **Subtitle** (TextView):
  - Text Size: `14sp`.
  - Text Color: `#757575`.
  - Placeholder text: "Start adding items to your shopping list.".
- **Browse Pantries Button** (MaterialButton):
  - Background Color: `#4CAF50`.
  - Text Color: `#FFFFFF`.
  - Text: "Browse Pantries".
  - Padding: `16dp` internally.
  - Content Description: "Browse pantries to add items".

#### 3. Bottom Summary Bar (`bottomSummaryBar` - CardView or LinearLayout):
- Fixed at the bottom of the screen.
- Background color: `#FFFFFF`.
- Elevation: `8dp`.
- Padding: `16dp` internally.
- **Subtotal Row** (LinearLayout - horizontal orientation):
  - Contains "Subtotal" TextView (left-aligned) and `tvSubtotalAmount` TextView (right-aligned).
  - Text Color: `#121212`.
- **Divider** (View):
  - Height: `1dp`.
  - Background Color: `#BDBDBD`.
  - Margins: `8dp` vertical.
- **Total Row** (LinearLayout - horizontal orientation):
  - Contains "Total" TextView (left-aligned).
    - Text Size: `18sp`.
    - Text Style: Bold.
    - Text Color: `#121212`.
  - **Total Amount** (`tvTotalAmount` - TextView):
    - Right-aligned.
    - Text Size: `20sp`.
    - Text Style: Bold.
    - Text Color: `#4CAF50`.
- **Checkout Button** (`btnCheckout` - MaterialButton):
  - Full width.
  - Background Color: `#4CAF50`.
  - Text Color: `#FFFFFF`.
  - Text: "Proceed to Checkout".
  - Text Size: `16sp`.
  - Text Style: Bold.
  - Padding: `16dp` internally.

#### 4. Bottom Navigation Bar (BottomNavigationView):
- Matches existing style from the Home Page.
- Contains icons for "Home", "Recipes", "Cart", "Profile".
- The "Cart" button should be in the active state, highlighted with a subtle pill or background tint using the primary green `#4CAF50`.
- Ensure proper sizing and contentDescription for all navigation items.

### General Spacing & Typography:

- **Padding**: Standard `16dp` to `24dp` for screen edges and major sections.
- **Card Margins**: `8dp` to `16dp`.
- **Element Spacing**: `4dp` to `8dp` between smaller elements.
- **Typography**: Use a modern sans-serif typeface (e.g., Material 3 default, Roboto, or Google Sans if available via custom fonts).
- Ensure all touch targets are at least `44dp`.
- Maintain AA contrast standards for all text and interactive elements.

### View IDs Summary:

- `topAppBar` - Top app bar container
- `btnBack` - Back button
- `tvCartTitle` - Cart title text
- `scrollView` - Main scrollable content
- `rvCartItems` - RecyclerView for cart items
- `emptyCartContainer` - Empty state container
- `ivEmptyCartIcon` - Empty cart icon
- `tvEmptyCartTitle` - Empty cart title
- `tvEmptyCartSubtitle` - Empty cart subtitle
- `btnBrowsePantries` - Browse pantries button
- `bottomSummaryBar` - Bottom summary container
- `tvSubtotalLabel` - Subtotal label (optional)
- `tvSubtotalAmount` - Subtotal amount
- `tvTotalLabel` - Total label (optional)
- `tvTotalAmount` - Total amount
- `btnCheckout` - Checkout button
- `bottomNavigation` - Bottom navigation bar

### File Naming:

- Main layout file: `activity_cart.xml`
- RecyclerView item layout: `item_cart_product.xml`

### Notes:

- Ensure all text uses string resources (for localization)
- The RecyclerView item layout should be a separate XML file
- Match styling consistency with `activity_home_page.xml` and `activity_profile.xml`
- Use proper Material Design icons from the app's drawable resources
- Support both portrait and landscape orientations
- Ensure proper accessibility with content descriptions

