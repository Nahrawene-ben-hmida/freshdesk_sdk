# freshdesk_sdk_example

Demonstrates how to use the freshdesk_sdk plugin.

## Getting Started

1. Update Freshdesk credentials in `lib/main.dart`:
   - Replace `YOUR_TOKEN`, `YOUR_HOST`, and `YOUR_SDK_ID` with your actual credentials
   - Optionally add a JWT token for authenticated users

2. Run the app:
   ```bash
   flutter run
   ```

## Features Demonstrated

- ✅ SDK initialization with configuration
- ✅ Show support chat interface
- ✅ Set user properties
- ✅ Set ticket properties
- ✅ Get unread message count
- ✅ Listen to unread count updates
- ✅ Track custom events
- ✅ Clear user data (logout)

## Getting Freshdesk Credentials

1. Log in to your [Freshdesk](https://www.freshdesk.com/) dashboard as admin
2. Navigate to **Admin → Channels → Mobile Chat SDK**
3. Create a new widget or select an existing one
4. Copy the `token`, `host`, and `sdkId` values
