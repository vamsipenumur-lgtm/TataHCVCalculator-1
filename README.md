# ValueMax (Android)

Offline Android app for Tata Motors Heavy Commercial Vehicle dealer sales executives.
Built with Kotlin + Jetpack Compose (Material 3). No internet permission — everything runs on-device.

## Three calculators

### 1. FE MaX — Fuel Savings (from FE_Series_FE_MaX.xlsx)
Inputs: vehicle/tonnage, monthly running (km), current mileage (km/l), diesel price (Rs/l).
Shows current fuel cost, then savings at 1–12% mileage improvement with a slider
defaulted to the FE Series 7–10% band. Outputs litres saved/month, Rs/month, Rs/year, Rs/5-years.

### 2. Revenue MaX — High Payload Trucks (from HPT_Revenue_MaX.xlsx)
Tonnage point dropdown (30T/37T/44T = +1.8T over base; 49T = +1.3T, editable),
% loaded running, yearly km, freight rate (Rs/tonne-km), years.
Annual advantage = additional payload x loaded% x yearly km x rate. Lifetime = annual x years.

### 3. BRT — Business Return Template (from Full_BRT.xlsx)
Two-product comparison (deliberately limited to 2 products) with full P&L:
freight earned, fixed costs (crew x 12 + insurance 4% of initial cost + admin),
running costs (fuel, DEF, tyres, maintenance, toll + per-tonne expenses),
EMI via reducing-balance PMT over (tenure x 12 - moratorium) months,
operating profit/year, cost/km, cost/ton-km, and extra profit with the Tata product
per year and over the chosen horizon.

#### Generalised tyre calculation (the key change vs the Excel)
The Excel BRT hardcoded the axle layout per column:
  - Columns C/D: ((tyre_cost x 2)/front_life) + (tyre_cost x 8/drive_life)  -> 10-tyre rigid
  - Column E: 2+4 radial + 2+4 bias with separate trailer-axle lives        -> tractor-trailer
That breaks as soon as the two products have different configurations.

In the app each product carries its own list of axle groups, each with
(tyre count, cost per tyre, life in km), and:

    tyre cost per km = SUM over groups of (count x cost / life)

Add/remove groups freely — so a 10-tyre MAV can be compared against a 14-tyre
tractor-trailer, each priced on its own tyre economics.


## Version 1.1 changes
- Customer name, location & route of operation captured in all three calculators and printed on PDFs
- Save estimates: every calculator has a "Save estimate" button; a Saved Estimates dashboard lists them (type, customer, location/route, date) and tapping an entry reopens it directly in its calculator; entries can be deleted
- Branding: official "TATA MOTORS Commercial Vehicles | Better Always" lockup (from the supplied template) used in the home header, PDF header and the app launcher icon
- Home screen cards are uniform height (incl. the new dashboard card)
- FE MaX slider fixed: snaps to whole percentages so every value 1-12% (incl. 6% & 8%) is selectable
- Years of operation is an input everywhere; all "5 years" wording/figures now follow the selected years
- Sharing now generates a branded PDF (native PdfDocument, offline) with proper tables, the campaign taglines from the creatives ("Go Further with Every Drop" / "Carry More. Earn More" / "Ab Profit Hoga Aur Bhi Zyaada") and the "TATA TRUCKS | DESH KE TRUCKS" footer; shared via FileProvider
- BRT highlighting is direction-aware (lower cost / EMI green, higher revenue / tons / profit green) and equal values are not highlighted on either side
- BRT shows "Tons carried / year" instead of Ton-km / year
- Drafts auto-save on every keystroke, so closing the app without saving restores the last entered values on next launch

## Version 1.2 changes
- Truck/creative imagery embedded in all three PDFs (offline, from bundled drawables):
  FE MaX -> FE Series Signa 5532.S tractor-trailer (tunnel creative);
  Revenue MaX -> "Higher Payload Trucks - Carry More. Earn More" red Signa lineup banner;
  BRT -> Tata truck range from the "Ab Profit Hoga Aur Bhi Zyaada" creative.
- BRT PDF: headline profit figure moved above the comparison table for better page flow.


## Version 1.3 — UX improvements
- Thousand-separator formatting on large number fields (Indian grouping, e.g. 10,00,000)
- Customer name required to save/share, with an inline error cue
- Reset button (with confirm) on every calculator
- Collapsible sections in the long BRT product form
- Sticky summary bar in BRT showing extra profit/year while scrolling inputs
- Dashboard search (customer / route / type) and sort (newest / name / type)
- BRT results: bar chart of profit/year alongside the table
- BRT results: cost-breakdown donut for the Tata product
- Count-up animation on hero figures
- Delta badges showing the advantage (e.g. ▲ ₹2.15 Lakh)
- "Show customer" summary view that hides inputs and shows only the punchline numbers
- Share as image (JPG) in addition to PDF
- In-app PDF preview before sharing, with a loading state

## Build

Requirements: Android Studio (Koala or newer) / JDK 17 / Android SDK 35.

    1. Open the TataHCVCalculator folder in Android Studio.
    2. Let Gradle sync (AGP 8.5.2, Kotlin 2.0.20, Compose BOM 2024.09).
    3. Run on a device/emulator (minSdk 24, i.e. Android 7.0+).

Command line:

    ./gradlew assembleDebug
    # APK at app/build/outputs/apk/debug/app-debug.apk

(No gradle wrapper jar is bundled here; generate one with `gradle wrapper
--gradle-version 8.7` or open in Android Studio which handles it.)

## Sharing with customers
Each calculator has a "Share" button that builds a plain-text summary and opens the
Android share sheet (WhatsApp, SMS, email...). Works offline — sending happens via
whatever app the dealer picks.

## Where the formulas live
All math is in `app/src/main/java/com/tatamotors/hcvcalculator/logic/` as pure Kotlin
objects (`FeMaxLogic`, `HptLogic`, `BrtLogic`) with the originating Excel cell
references documented in comments — easy to audit against the spreadsheets and to
unit-test.

## Verified against the spreadsheets
- FE MaX @ 10,000 km, 3.1 km/l, Rs 100/l: 5% -> Rs 1,84,332/yr; 10% -> Rs 3,51,906/yr (matches sheet)
- Revenue MaX: +1.8T -> Rs 2,92,500/yr; +1.3T -> Rs 2,11,250/yr (matches sheet)
- BRT tyre cost (2 front + 8 drive @ Rs 25,000 / 1,00,000 km) -> Rs 2.50/km (matches sheet C30)
