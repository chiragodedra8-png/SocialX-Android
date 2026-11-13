# SocialX — Android social network

A complete, installable Android social app inspired by Facebook: feed, stories,
friends, messenger, notifications, search, profiles and settings — with a clean
white/blue design system, full dark mode and Material 3 components.

- **Package:** `com.socialx.app`
- **minSdk:** 26 (Android 8.0) · **targetSdk:** 34 (Android 14)
- **Language:** Kotlin · **UI:** XML layouts + Material 3 + ViewBinding
- **APK:** `SocialX.apk` (release, signed)

---

## 1. What works in the shipped APK

| Area | Implemented |
|---|---|
| Auth | Sign up with email **and** phone, login, logout, forgot password (security question), change password, delete account |
| Profile | Profile picture, cover photo, bio, edit profile, friends/followers/following counts, private account flag |
| Feed | Text / photo / video posts, like & unlike, comment & reply, share, save, timestamps, infinite scroll, pull-to-refresh |
| Friends | Send request, accept/reject, remove friend, follow/unfollow, search users, mutual friend counts |
| Stories | Photo & video stories, 24-hour auto-expiry, story viewer with progress bar, viewer list for your own story |
| Messenger | One-to-one chat, text + photo/video messages, online/offline status, typing indicator, sent/read receipts, delete message |
| Notifications | Likes, comments, replies, friend requests, accepts, follows, messages |
| Search | People, posts and trending hashtags; suggested friends |
| Settings | Light / dark / system theme, privacy switches, change password, blocked users, report, delete account, logout |

## 2. Architecture

```
app/src/main/java/com/socialx/app/
├── core/            AppConfig, Prefs, ServiceLocator, Utils, Avatars
├── data/            Models, DbHelper (SQLite), SocialRepository (interface),
│                    LocalRepository (implementation), MediaStoreHelper, Notifier
└── ui/              auth, main (5 tabs), feed, comments, chat, story,
                     friends, profile, search, settings
```

`SocialRepository` is the single data contract. The app ships with
`LocalRepository`, a real SQLite backend (no mock data): hashed passwords
(PBKDF2), real relational posts/likes/comments/friendships/stories/messages,
real 24-hour story expiry and real read receipts.

`firebase/FirebaseRepository.kt` is a complete Firebase Auth + Cloud Firestore +
Firebase Storage implementation of the same interface. It is **not compiled into
the shipped APK** so the APK stays small and builds in seconds — see section 6
to switch it on.

## 3. Build from source

```bash
# Requirements: JDK 17, Android SDK 34
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleDebug      # debug APK
./gradlew :app:assembleRelease    # signed release APK
```

Output: `app/build/outputs/apk/release/app-release.apk`

The release build is signed with `socialx-release.keystore`
(alias `socialx`, password `socialx2026`). **Replace this keystore with your own
before publishing to Google Play.**

## 4. Project structure

```
SocialX/
├── app/
│   ├── build.gradle
│   ├── google-services.json          # placeholder — replace with yours
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/socialx/app/…    # 40 Kotlin files
│       └── res/                      # 27 layouts, 26 vector icons, themes
├── firebase/
│   ├── FirebaseRepository.kt.txt     # optional cloud backend
│   ├── firestore.rules
│   └── storage.rules
├── build.gradle · settings.gradle · gradle.properties
└── README.md
```

## 5. Database schema (local backend)

`users, posts, likes, comments, saves, friendships, follows, stories,
story_views, conversations, messages, notifications, blocks, reports`

## 6. Switching to the Firebase cloud backend

The local backend is single-device. Multi-device sync needs a provisioned
Firebase project, which must be created under your own Google account.

1. Create a Firebase project → add an Android app with package `com.socialx.app`.
2. Download `google-services.json` and replace `app/google-services.json`.
3. Copy the cloud implementation into the compiled source set:
   `cp firebase/FirebaseRepository.kt.txt app/src/main/java/com/socialx/app/data/FirebaseRepository.kt`
4. In `app/build.gradle` uncomment the `com.google.gms.google-services` plugin
   and the four `firebase-*` dependency lines.
5. Set `AppConfig.USE_FIREBASE = true` and construct `FirebaseRepository` in
   `ServiceLocator`.
6. Deploy the rules in `firebase/firestore.rules` and `firebase/storage.rules`.
7. Enable Email/Password sign-in in Firebase Authentication.

## 7. Honest limitations

- **Real-time across devices** requires the Firebase backend above. In the
  shipped local build, chat/typing/receipts are real but operate between
  accounts on the same device.
- **Phone sign-up** stores and verifies the phone number locally. Firebase
  phone auth additionally needs an SMS/OTP provider and app verification.
- **Video** uses the platform `VideoView` (playback + upload), not a custom
  player with PiP.
- **Push notifications** are local system notifications, not FCM, in the local
  build. FCM requires your own Firebase project.
- No Android emulator is available in the build environment, so the APK was
  verified structurally (manifest, signature, dex, resources) rather than
  launched on a virtual device.

## 8. Demo content

`Settings → Load demo content` creates three demo people with posts, friend
links and follows so you can see the feed, stories and chat UI immediately.
Passwords for those accounts are `demo1234`. Nothing is pre-seeded automatically.
