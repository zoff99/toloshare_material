# ToLoShare

ToLoShare is a cross-platform desktop application for secure peer-to-peer real-time location sharing.<br>
The app is specifically designed as a Tox protocol client, emphasizing privacy<br>
and decentralization through its P2P architecture.<br>
The "Material" in the name refers to the Material Design UI components used in the Compose interface.

<img src="https://github.com/zoff99/toloshare_material/releases/download/nightly/screenshot-macos-15arm.png" width="90%">

Automated screenshots:<br>
<img src="https://github.com/zoff99/toloshare_material/releases/download/nightly/screenshot-macos-14arm.png" height="230"></a>
<img src="https://github.com/zoff99/toloshare_material/releases/download/nightly/screenshot-windows.png" height="230"></a>
<img src="https://github.com/zoff99/toloshare_material/releases/download/nightly/screenshot-linux.png" height="230"></a>

## Build Status

**Github:** [![Android CI](https://github.com/zoff99/toloshare_material/workflows/Nightly/badge.svg)](https://github.com/zoff99/toloshare_material/actions?query=workflow%3A%22Nightly%22)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.en.html)
[![Liberapay](https://img.shields.io/liberapay/goal/zoff.svg?logo=liberapay)](https://liberapay.com/zoff/donate)
</a>&nbsp;[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/zoff99/toloshare_material)

&nbsp;&nbsp;&nbsp;&nbsp;Looking for ToLoShare Mobile version? [follow me](https://github.com/zoff99/ToLoShare)


## Viewing Friends on the Map

Switch to MAP mode in the app<br>
Your connected friends locations appear as markers with their names<br>
Each marker shows:<br>
* Friend's name and location provider
* Speed in km/h
* Location age (turns red if older than 2 minutes)
* Direction arrow if bearing data is available

## Following a Friend on the map

Click on any friend's marker on the map<br>
The map will automatically center on that friend and track their movement<br>
A pin icon appears on the followed friend's marker<br>
To stop following, click the same marker again, or click on another friend's marker.

## Friend Location updates are not smooth, why?

Smooth updates only work for the currently followed friend,<br>
other friends locations update when a new location update comes in.

## GPX Route recording

* Start Recording: Click the red button to enable recording mode
* Select Friend: Click on a friend's marker on the map to start tracking their location
* GPX File Creation: When a friend is selected while recording is active, a new GpxWriter instance is created with a timestamped filename
* Location Tracking: As location data arrives from the network, points are automatically added to the GPX file if the selected friend matches the incoming location data
* Stop Recording: Click the same friend marker again to stop tracking, or click the red button to disable recording mode entirely


## 🚀 Featured Applications

Join a growing community of security-conscious people. Check out these featured applications:

*   **[TRIfA](https://github.com/zoff99/ToxAndroidRefImpl)**: The Tox flagship secure messenger for Android.
*   **[TRIfA for Desktop](https://github.com/Zoxcore/trifa_material)**: The feature rich Tox Desktop Messaging Client.
*   **[Tox Push Msgs](https://github.com/zoff99/tox_push_msg_app)**: The Companion App for TRIfA and TRIfA for Desktop to enable Push Messages.
*   **[ToxProxy](https://github.com/zoff99/ToxProxy)**: Offline message relay functionality for TRIfA and TRIfA for Desktop.
*   **[ToLoShare](https://github.com/zoff99/ToLoShare)**: A specialized Android application for secure, peer-to-peer real-time location sharing.
*   **[ToLoShare for Desktop](https://github.com/zoff99/ToLoShare_material)**: A cross-platform desktop application for secure peer-to-peer real-time location sharing.
*   **[ToFShare](https://github.com/zoff99/ToFShare)**: Secure decentralized file sharing for Android using the Tox protocol.
*   **[tox_videoplayer](https://github.com/zoff99/tox_videoplayer)**: A command-line application that streams video and audio content over the Tox network.


<br>
Any use of this project's code by GitHub Copilot, past or present, is done
without our permission.  We do not consent to GitHub's use of this project's
code in Copilot.
<br>
No part of this work may be used or reproduced in any manner for the purpose of training artificial intelligence technologies or systems.
