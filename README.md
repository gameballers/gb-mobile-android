

1. File >> New >> New Module
2. Select Import .JAR/.AAR Package
3. Click Next
4. Fill the file name with gameballsdk-release.aar full path
5. Click Finish
6. Add this dependencies to application dependencies:

         implementation 'com.google.firebase:firebase-core:16.0.1'
         implementation 'com.google.firebase:firebase-messaging:17.3.0'
         implementation 'io.reactivex.rxjava2:rxjava:2.2.1'
         implementation 'com.squareup.retrofit2:retrofit:2.4.0'
         implementation 'com.google.code.gson:gson:2.8.2'
         implementation 'com.squareup.retrofit2:converter-gson:2.3.0'
         implementation 'com.squareup.retrofit2:adapter-rxjava2:2.3.0'
         implementation 'com.squareup.okhttp3:okhttp:3.10.0'
         implementation project(':gameballsdk-release')

	
**Note**
	check that the above dependencies did not conflict with your dependencies.

7. Add this line to Application Class

         GameBallApp.getInstance(this).init(CLIENT_ID, EXTERNAL_ID,NOTIFICATION_ICON);

    CLIENT_ID: is the your client id provided by GameBall.
    EXTERNAL ID: is your user Id.
    NOTIFICATION_ICON: is the desire notification icon
	
8. If you are using firebase add this line to the start of onMessageReceived

        if(GameBallApp.getInstance(this).isGameBallNotification(remoteMessage)) return;

   If not copy the GameBallDemoFirebaseMessagingService class to your java package folder.
   
9. Add this lines to your AndroidManifest.xml inside the application tag

         <activity android:name="com.gameball.gameball.views.GameBallActivity">
         		<intent-filter>
         			<action android:name="GAME_BALL_SDK" />
         			<category android:name="android.intent.category.DEFAULT" />
         		</intent-filter>
         </activity>

	
10. If you are not using firebase add this lines to your AndroidManifest.xml inside the application tag
	
         <service android:name=".GameBallDemoFirebaseMessagingService">
                  <intent-filter>
                       <action android:name="com.google.firebase.MESSAGING_EVENT" />
                  </intent-filter>
         </service>

		
**Note** 
	attached sample files for integration
	

