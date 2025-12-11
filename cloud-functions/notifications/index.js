/**
 * Firebase Cloud Functions for Pantry Pal Push Notifications
 * 
 * These functions automatically send push notifications when:
 * 1. A new pantry is created (onPantryCreated)
 * 2. Items are added to an existing pantry (onPantryUpdated)
 * 
 * Deployment:
 * 1. Install dependencies: npm install
 * 2. Deploy: firebase deploy --only functions
 * 
 * See PUSH_NOTIFICATIONS_SETUP.md for detailed setup guide
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

/**
 * Triggered when a new pantry is created
 * Sends notification to all users
 */
exports.onPantryCreated = functions.firestore
    .document('pantries/{pantryId}')
    .onCreate(async (snap, context) => {
        const pantryData = snap.data();
        const pantryId = context.params.pantryId;
        const pantryName = pantryData.name || 'New Pantry';
        const pantryAddress = pantryData.address || '';

        console.log(`New pantry created: ${pantryName} (${pantryId})`);

        // Get all user FCM tokens
        const usersSnapshot = await admin.firestore().collection('users').get();
        const tokens = [];

        usersSnapshot.forEach(doc => {
            const token = doc.data().notificationToken;
            if (token && token.trim() !== '') {
                tokens.push(token);
            }
        });

        if (tokens.length === 0) {
            console.log('No FCM tokens found');
            return null;
        }

        // Prepare notification message
        const addressText = pantryAddress && pantryAddress.trim() !== '' ? ` at ${pantryAddress}` : '';
        const message = {
            notification: {
                title: '🍽️ New Pantry Available!',
                body: `${pantryName} has been posted${addressText}`
            },
            data: {
                type: 'pantry',
                pantryId: pantryId,
                pantryName: pantryName,
                click_action: 'FLUTTER_NOTIFICATION_CLICK'
            },
            tokens: tokens
        };

        // Send notification
        try {
            const response = await admin.messaging().sendEachForMulticast(message);
            console.log(`Successfully sent ${response.successCount} notifications`);
            console.log(`Failed: ${response.failureCount}`);
            
            // Clean up invalid tokens
            if (response.failureCount > 0) {
                const failedTokens = [];
                response.responses.forEach((resp, idx) => {
                    if (!resp.success) {
                        failedTokens.push(tokens[idx]);
                    }
                });
                console.log(`Failed tokens: ${failedTokens.length}`);
            }
        } catch (error) {
            console.error('Error sending notifications:', error);
        }

        return null;
    });

/**
 * Triggered when items are added to a pantry
 * Sends notification to all users
 */
exports.onPantryUpdated = functions.firestore
    .document('pantries/{pantryId}')
    .onUpdate(async (change, context) => {
        const newData = change.after.data();
        const oldData = change.before.data();
        const pantryId = context.params.pantryId;
        const pantryName = newData.name || 'Pantry';

        // Check if items were added by comparing donors/items count
        // For now, we'll trigger on any update (you can make this more specific)
        const oldDonorsCount = oldData.donorsCount || 0;
        const newDonorsCount = newData.donorsCount || 0;

        // Check if items were added by querying donors subcollection
        const pantryRef = admin.firestore().collection('pantries').doc(pantryId);
        const donorsSnapshot = await pantryRef.collection('donors').get();
        
        // Count total items
        let totalItems = 0;
        for (const donorDoc of donorsSnapshot.docs) {
            const itemsSnapshot = await donorDoc.ref.collection('items').get();
            totalItems += itemsSnapshot.size;
        }

        // Only send notification if there are items
        if (totalItems === 0) {
            return null;
        }

        console.log(`Pantry ${pantryName} has ${totalItems} items available`);

        // Get all user FCM tokens
        const usersSnapshot = await admin.firestore().collection('users').get();
        const tokens = [];

        usersSnapshot.forEach(doc => {
            const token = doc.data().notificationToken;
            if (token && token.trim() !== '') {
                tokens.push(token);
            }
        });

        if (tokens.length === 0) {
            console.log('No FCM tokens found');
            return null;
        }

        const message = {
            notification: {
                title: '📦 New Items Added!',
                body: `${pantryName} now has ${totalItems} item(s) available`
            },
            data: {
                type: 'item',
                pantryId: pantryId,
                pantryName: pantryName,
                itemCount: totalItems.toString()
            },
            tokens: tokens
        };

        try {
            const response = await admin.messaging().sendEachForMulticast(message);
            console.log(`Successfully sent ${response.successCount} item notifications`);
        } catch (error) {
            console.error('Error sending item notifications:', error);
        }

        return null;
    });

