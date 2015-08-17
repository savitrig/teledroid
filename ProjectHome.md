# Welcome to Teledroid Project #

The utimate goal of Teledroid is to provide a mobile env to access your cloud computing elements anywhere and anytime.

Enjoy!

The _Teledroid Project Team_
## Introduction ##

The cloud computing / distributed computing systems have made it possible to extend both functionality and battery life of mobile devices. The storage element no doubt plays an important role in these computing modals. A tool that is able to access and manage the files on a remote system will be of great help on the Android platform. Our plan is to implement a synchronized user space file system so that we could use the remote computing platform to make the android devices to be a more powerful palmar workstation.

To serve the need of remote computing task, the application should be able to provide a file system to be mounted into Android, capable of operating on files on a remote computer. Further more, the user should be able to mirror selected files and folders into local file system as cache, i.e internal storage or external storage card. These cached files will be synchronized with remote system. Then, the final goal is to allow files created or modified on Android be able to sync to remote storage system and processed by services on remote server, then the result stored in remote storage system sync back to Android.

For market reasons, there are several cloud storage providers using different interface / protocols. At the initial launch of our application, we avoid the mass of supporting these incompatible systems. We choose to implement our system for the widely used SSH protocol. Through sshfs, we will be able to mount the remote unix system and through ssh we will be able to launch the remote services. Still, when we construct our application framework, we intend to use the module / plugin structure. This mechanism will allow us to introduce the support for other cloud computing platform later.

## Mile Stones ##
  * M1. Integrate FUSE and sshfs
  * M2. Mirror, Sync and Interface
  * M3. Remote Execution
  * ME. Future features

More info about mile stones please refer to Roadmap