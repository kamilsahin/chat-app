# Chat App — Requirements

## Overview

Real-time chat application built with Java Spring Boot, WebSocket, and MongoDB. Mobile client built with Flutter.

---

## Functional Requirements

### Authentication
- User registration with username, email and password
- Login / logout
- JWT-based authentication
- 

### Users
- User profile (avatar, display name, bio)
- Online / offline status
- Last seen timestamp
- 

### 1-1 Chat
- Start a private conversation with any user
- Real-time message delivery via WebSocket
- Message history stored in MongoDB
- 

### Group Chat
- Create a group with a name and optional avatar
- Add / remove members
- Group admin role
- Real-time message delivery to all members via WebSocket
- Message history stored in MongoDB
- 

### Messaging
- Send text messages
- Reply to a specific message (threaded reply)
- React to a message with an emoji
- Delete a message (soft delete — shows "message deleted")
- Read receipts (delivered / seen)
- 

### Push Notifications
- Notify user of new messages when the app is in background or closed
- Notify on group mentions (@username)
- 

---

## Non-Functional Requirements

- Messages must be delivered in real-time (< 500ms under normal conditions)
- Message history must be paginated
- 

---

## Tech Stack

| Layer         | Technology                     |
|---------------|--------------------------------|
| Backend       | Java 21, Spring Boot 3         |
| Real-time     | WebSocket (STOMP)              |
| Database      | MongoDB                        |
| Auth          | JWT                            |
| Notifications | Firebase Cloud Messaging (FCM) |
| Container     | Docker                         |
| Mobile        | Flutter                        |

---

## Out of Scope (v1)

- Voice / video calls
- File / image sharing
- Message forwarding
