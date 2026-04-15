# Chat App — Requirements

## Overview

An open-source, self-hosted real-time chat application. Any project can download this repository, configure it for their own environment, and deploy it on their own server. Each deployment is fully independent — its own database, its own credentials, its own data.

**Target use cases:**
- A social platform (e.g. ActiZone) embedding chat into their existing app
- A new project (e.g. CampZone) spinning up chat from scratch
- Any application needing real-time messaging without building it from scratch

---

## Deployment Model

```
Developer clones the repo
        │
        ├── Fills in configuration (application.yml / .env)
        │     - MongoDB connection string
        │     - JWT secret (from their own auth system)
        │     - FCM credentials
        │     - Storage provider
        │     - Feature flags
        │
        ├── docker-compose up
        │
        └── Chat App running on their own server
              - Their own MongoDB
              - Their own FCM project
              - Their own storage bucket
              - Fully isolated from other deployments
```

---

## Integration Model

The Chat App does **not** handle user registration or login. It expects the host application to manage users and issue JWTs. The Chat App validates those tokens.

```
Host App Backend
        │
        │  1. User logs in → Host App issues JWT
        │  2. Host App calls Chat App Internal API
        │     to create rooms and manage members
        │
        ▼
   Chat App (Internal API)
   - Room lifecycle (create/delete/members)
   - Protected by a shared Internal API secret
        │
        │  3. Flutter / Web client connects directly
        │     to Chat App using the JWT from step 1
        ▼
   Chat App (Client API + WebSocket)
   - Messaging, presence, history
```

### Why this separation?

The host application owns the business logic — it knows when to create a chat room (e.g. when an activity is created) and who should be in it (e.g. activity participants). The Chat App only handles messaging.

---

## Configuration

All configuration lives in `application.yml` (or equivalent `.env`). The deploying developer fills this in once before running the app.

```yaml
chat:
  # Auth
  auth:
    jwt-secret: "your-jwt-secret"          # Must match your host app's JWT signing secret
    jwt-issuer: "your-app-name"            # Optional: validate JWT issuer claim

  # Internal API protection
  internal:
    api-secret: "your-internal-secret"     # Shared secret between host backend and chat app

  # MongoDB
  database:
    uri: "mongodb://localhost:27017/chatdb"

  # Storage
  storage:
    provider: LOCAL | S3 | R2              # Choose one
    local:
      upload-dir: "/uploads"
    s3:
      bucket: ""
      region: ""
      access-key: ""
      secret-key: ""
    r2:
      bucket: ""
      account-id: ""
      access-key: ""
      secret-key: ""

  # Push Notifications
  notifications:
    enabled: true
    fcm:
      credentials-file: "path/to/firebase-service-account.json"

  # Feature Flags
  features:
    file-sharing-enabled: true
    reactions-enabled: true
    max-message-length: 2000
    max-group-size: 500
    message-retention-days: 0             # 0 = keep forever
    delete-permission: OWN_ONLY | ADMIN_ANY
    keyword-blocklist:
      - "badword1"
      - "badword2"
```

---

## Functional Requirements

### Authentication
- Chat App does **not** register or log in users
- Clients authenticate using a JWT issued by the host application
- JWT must contain: `userId`, `displayName` (configurable claim names)
- Chat App validates the JWT signature using the configured `jwt-secret`
- On first valid connection, a local user profile is auto-created if it doesn't exist

### Users
- User profile: avatar URL, display name, bio
- Profile can be updated by the user via Client API
- Online / offline status
- Last seen timestamp

### Rooms
- Two types: **direct** (1-1) and **group**
- Direct rooms: created by the client
- Group rooms: created via Internal API (by the host application) or by the client
- Group rooms have a name, optional avatar, and admin role
- Only room members can read and send messages

### Messaging
- Send and receive text messages in real-time
- Edit a sent message (edited messages show an "edited" indicator)
- Reply to a specific message (single-level threaded reply)
- Forward a message to another room
- React to a message with an emoji (configurable on/off)
- Delete a message — soft delete, content replaced with `"Bu mesaj silindi"`
    - Sender can always delete their own message
    - Admin can delete any message in the room
- Read receipts: delivered / seen
- Unread message count per room (badge)
- Image sharing (configurable on/off; images only, no arbitrary file types in v1)
- Location sharing — sends coordinates + address text; rendered as a tappable map preview in the client
- Link sharing — URL sent as plain text (no preview in v1)
- Pin a message in a room (admin only; pinned message visible at top of room)

### Presence
- Online/offline tracked via WebSocket connect/disconnect + heartbeat
- Last seen timestamp updated on disconnect
- Presence events broadcast to room members

### Notifications & Mute
- Mute a room: push notifications suppressed, unread badge continues to increment
- Mute durations: 8 hours / 1 week / indefinite (until manually unmuted)
- Muted rooms show a mute icon in the room list
- Configurable keyword blocklist
- Matched words replaced with `***` before storage and delivery

### Push Notifications
- FCM-based, credentials configured per deployment
- Notify on new message when app is in background or closed
- Notify on group mention (`@username`)
- Can be disabled entirely via config

---

## Internal API

Protected by `X-Internal-Secret` header. Called only by the host application's backend — never directly by the mobile/web client.

### Room Lifecycle
- `POST /internal/rooms` — Create a room
- `DELETE /internal/rooms/{roomId}` — Delete a room
- `POST /internal/rooms/{roomId}/members` — Add member(s)
- `DELETE /internal/rooms/{roomId}/members/{userId}` — Remove a member

### User Sync (optional)
- `PUT /internal/users/{userId}` — Push updated user info (display name, avatar) from host app
- `DELETE /internal/users/{userId}` — Remove user data (e.g. on account deletion)

---

## Client API

Called by Flutter / web client. Requires a valid JWT in `Authorization: Bearer` header.

### Rooms
- `GET /api/rooms` — List rooms for current user
- `GET /api/rooms/{roomId}` — Room details and member list
- `PUT /api/rooms/{roomId}/mute` — Mute a room (body: duration — `8h / 1w / indefinite`)
- `DELETE /api/rooms/{roomId}/mute` — Unmute a room

### Messages
- `GET /api/rooms/{roomId}/messages` — Paginated message history (cursor-based, 50 per page)
- `PATCH /api/messages/{messageId}` — Edit message content (sender only)
- `DELETE /api/messages/{messageId}` — Soft delete (sender or admin)
- `PUT /api/rooms/{roomId}/pin/{messageId}` — Pin a message (admin only)
- `DELETE /api/rooms/{roomId}/pin` — Unpin current pinned message (admin only)

### Users
- `GET /api/users/{userId}` — Get user profile
- `PUT /api/users/me` — Update own profile

---

## Real-Time (WebSocket / STOMP)

### Connection
- On-demand: opened when entering a room, closed on exit
- Endpoint: `ws://{host}/ws`
- Auth: JWT passed in STOMP CONNECT header

### Topics

| Topic | Direction | Description |
|---|---|---|
| `/topic/room.{roomId}` | Subscribe | Incoming messages |
| `/topic/room.{roomId}.presence` | Subscribe | Member presence events |
| `/app/room.{roomId}.send` | Publish | Send a message |
| `/app/room.{roomId}.typing` | Publish | Typing indicator |
| `/user/queue/notifications` | Subscribe | Personal notifications (mention, reply) |

### Broker
- Spring in-memory STOMP broker (default, sufficient for most deployments)
- Redis Pub/Sub supported for horizontal scaling (configured via `spring.redis.*`)

---

## Data Model (MongoDB)

### `users`
```json
{
  "_id": "userId",
  "externalId": "id from host app JWT",
  "displayName": "Kamil",
  "avatarUrl": "...",
  "bio": "...",
  "isOnline": true,
  "lastSeen": "2025-01-01T10:00:00Z",
  "createdAt": "..."
}
```

### `rooms`
```json
{
  "_id": "roomId",
  "type": "GROUP | DIRECT",
  "name": "Ankara Bisiklet Turu",
  "avatarUrl": "...",
  "members": [
    {
      "userId": "u1",
      "role": "ADMIN",
      "joinedAt": "...",
      "muted": true,
      "mutedUntil": "2025-01-08T10:00:00Z | null"
    },
    { "userId": "u2", "role": "MEMBER", "joinedAt": "...", "muted": false, "mutedUntil": null }
  ],
  "createdAt": "..."
}
```

### `messages`
```json
{
  "_id": "messageId",
  "roomId": "roomId",
  "senderId": "userId",
  "type": "TEXT | IMAGE | LOCATION",
  "content": "Merhaba!",
  "replyTo": "messageId | null",
  "forwardedFrom": "messageId | null",
  "reactions": [
    { "emoji": "👍", "userIds": ["u1", "u2"] }
  ],
  "readBy": [
    { "userId": "u1", "readAt": "..." }
  ],
  "isDeleted": false,
  "isEdited": false,
  "editedAt": "... | null",
  "imageUrl": "... | null",
  "location": {
    "latitude": 39.9334,
    "longitude": 32.8597,
    "address": "Kızılay, Ankara"
  },
  "isPinned": false,
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

## Non-Functional Requirements

- Messages delivered in real-time (< 500ms under normal conditions)
- Message history paginated (cursor-based, 50 messages per page)
- Single deployable artifact (JAR + Docker image)
- One-command startup via Docker Compose
- JWT secret and internal API secret never logged

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3 |
| Real-time | WebSocket (STOMP) |
| Message Broker | Spring in-memory (→ Redis Pub/Sub for scale) |
| Database | MongoDB |
| Auth | JWT (host app issues, chat app validates) |
| Notifications | Firebase Cloud Messaging (FCM) |
| File Storage | Local / AWS S3 / Cloudflare R2 (configurable) |
| Container | Docker + Docker Compose |
| Mobile Client | Flutter |

---

## Out of Scope (v1)

- Voice / video calls
- Voice messages
- Full-text message search
- Link / URL preview
- File sharing (non-image)
- User blocking
- System messages (e.g. "Kamil joined the group")
- Analytics / reporting
- Webhook events
- Read-only broadcast channels
- Multi-instance deployment without Redis (horizontal scaling)
- Admin UI (configuration is file-based)
