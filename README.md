# chat-app

A self-hosted, embeddable real-time chat backend. Built for teams that already have a running application and want to add chat without adopting a third-party service. Your existing app manages users and authentication — this service handles everything chat-related.

## Who is this for?

- **Developers with an existing backend** (Spring Boot, Django, Laravel, etc.) who want to add chat to their app without a SaaS dependency
- **Mobile/web apps** that need real-time messaging, group chats, and push notifications
- Use cases: activity-based group chats, direct messaging, support chats, community rooms

## How it works

Your backend creates users and rooms via the Internal API (server-to-server, protected by a shared secret). Your frontend authenticates with a JWT that your backend issues — the same token is used directly with this service. No separate login flow needed.

```
Your Backend ──(Internal API)──▶ chat-app ◀──(JWT + WebSocket)── Mobile/Web App
```

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 17, Spring Boot 3 |
| Database | MongoDB |
| Real-time | WebSocket / STOMP over SockJS |
| Auth | JWT (HS256) |
| Push notifications | Firebase Cloud Messaging (FCM) |
| File storage | Local filesystem (configurable path) |
| Containerization | Docker + Docker Compose |

## Features

- Real-time messaging via WebSocket (STOMP)
- Direct and group rooms
- Message reply, edit, soft-delete
- Emoji reactions
- Photo sharing (image upload + inline display)
- Typing indicators
- Push notifications via FCM (opt-in, per-user mute support)
- Pinned messages
- Unread count tracking
- Mute rooms (1h / 8h / 1 week / permanent)
- Keyword blocklist
- Configurable delete permissions (own messages only or admin can delete any)

## Quick Start

### 1. Clone and configure

```bash
git clone https://github.com/youruser/chat-app.git
cd chat-app/backend
cp .env.example .env
```

Edit `.env`:

```env
MONGO_URI=mongodb://admin:secret@localhost:27017/chatdb?authSource=admin
JWT_SECRET=your-jwt-secret-minimum-32-characters        # must match your main app
INTERNAL_API_SECRET=your-internal-api-secret-minimum-32-chars
```

### 2. Start with Docker Compose

```bash
docker-compose up -d
```

Runs on `http://localhost:8080` by default.

### 3. Sync a user

Call from your backend when a user registers or logs in:

```bash
curl -X PUT http://localhost:8080/internal/users/user123 \
  -H "X-Internal-Secret: your-internal-api-secret" \
  -H "Content-Type: application/json" \
  -d '{"displayName": "Jane Doe", "avatarUrl": "https://..."}'
```

### 4. Create a room

```bash
curl -X POST http://localhost:8080/internal/rooms \
  -H "X-Internal-Secret: your-internal-api-secret" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "DIRECT",
    "name": "Jane & John",
    "memberIds": ["user123", "user456"],
    "adminId": "user123"
  }'
```

Save the returned `id` — you'll need it to navigate users to the right room.

### 5. Generate a JWT

Your existing auth system issues the JWT. The `sub` claim must be the user's `externalId`:

```json
{
  "sub": "user123"
}
```

Sign with the same `JWT_SECRET`. The Flutter/web client uses this token directly when connecting.

---

## API Reference

All endpoints that require auth expect `Authorization: Bearer <jwt>` header.  
Internal endpoints expect `X-Internal-Secret: <secret>` header.

### Internal API — User Management

| Method | Path | Description |
|--------|------|-------------|
| `PUT` | `/internal/users/{externalId}` | Create or update a user (displayName, avatarUrl, bio) |
| `DELETE` | `/internal/users/{externalId}` | Delete a user |

### Internal API — Room Management

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/internal/rooms` | Create a room → returns full Room object with `id` |
| `DELETE` | `/internal/rooms/{roomId}` | Delete a room and its messages |
| `POST` | `/internal/rooms/{roomId}/members` | Add members to a room |
| `DELETE` | `/internal/rooms/{roomId}/members/{userId}` | Remove a member from a room |

**Create room request body:**
```json
{
  "type": "DIRECT",
  "name": "Room name",
  "memberIds": ["user1", "user2"],
  "adminId": "user1"
}
```
`type` is `DIRECT` or `GROUP`. `adminId` is optional — if omitted, all members get `MEMBER` role.

### User API

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/users/{userId}` | Get user profile |
| `PUT` | `/api/users/me` | Update own profile (displayName, avatarUrl, bio) |
| `PUT` | `/api/users/me/fcm-token` | Register FCM push token |

### Room API

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/rooms` | List rooms for the authenticated user |
| `GET` | `/api/rooms/{roomId}` | Get a single room |
| `PUT` | `/api/rooms/{roomId}/mute` | Mute a room (body: `{"duration": "PT1H"}`) |
| `DELETE` | `/api/rooms/{roomId}/mute` | Unmute a room |

**Mute durations:** `PT1H` (1 hour), `PT8H` (8 hours), `P7D` (1 week), omit for permanent.

### Message API

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/rooms/{roomId}/messages` | Get message history (paginated, `?cursor=<ISO instant>`) |
| `POST` | `/api/rooms/{roomId}/messages/image` | Upload and send a photo (`multipart/form-data`, field: `file`) |
| `PATCH` | `/api/messages/{messageId}` | Edit a message (body: `{"content": "new text"}`) |
| `DELETE` | `/api/messages/{messageId}` | Soft-delete a message |
| `GET` | `/api/rooms/{roomId}/pin` | Get pinned message |
| `PUT` | `/api/rooms/{roomId}/pin/{messageId}` | Pin a message (admin only) |
| `DELETE` | `/api/rooms/{roomId}/pin` | Unpin (admin only) |

### WebSocket (STOMP)

Connect to `ws://host/ws` with the JWT as a query parameter or STOMP header.

| Destination | Direction | Description |
|---|---|---|
| `/app/room.{roomId}.send` | Client → Server | Send a message |
| `/app/room.{roomId}.typing` | Client → Server | Send typing indicator |
| `/app/room.{roomId}.reaction` | Client → Server | Toggle emoji reaction |
| `/app/room.{roomId}.read` | Client → Server | Mark message as read |
| `/topic/room.{roomId}` | Server → Client | Receive messages, edits, deletes, reactions |
| `/topic/room.{roomId}.typing` | Server → Client | Receive typing events |

**Send message payload:**
```json
{
  "type": "text",
  "content": "Hello!",
  "replyTo": "optionalMessageId"
}
```

---

## Configuration

All configuration is done via environment variables. See `.env.example` for the full list.

| Variable | Default | Description |
|---|---|---|
| `MONGO_URI` | `mongodb://localhost:27017/chatdb` | MongoDB connection string |
| `JWT_SECRET` | — | HS256 signing secret (min 32 chars) |
| `JWT_ISSUER` | _(any)_ | Expected `iss` claim, leave empty to skip check |
| `INTERNAL_API_SECRET` | — | Secret for server-to-server calls |
| `CHAT_UPLOAD_DIR` | `./uploads` | Directory for uploaded images |
| `NOTIFICATIONS_ENABLED` | `false` | Enable FCM push notifications |
| `FCM_CREDENTIALS_FILE` | — | Path to Firebase service account JSON (`classpath:` prefix supported) |
| `FEATURE_FILE_SHARING` | `true` | Allow photo uploads |
| `FEATURE_REACTIONS` | `true` | Allow emoji reactions |
| `FEATURE_MAX_MSG_LENGTH` | `2000` | Max characters per message |
| `FEATURE_DELETE_PERMISSION` | `OWN_ONLY` | `OWN_ONLY` or `ADMIN_ANY` |
| `FEATURE_KEYWORD_BLOCKLIST` | — | Comma-separated words to censor |

## Push Notifications

1. Set `NOTIFICATIONS_ENABLED=true` in `.env`
2. Place your Firebase service account JSON and set `FCM_CREDENTIALS_FILE`
3. Each client registers its FCM token via `PUT /api/users/me/fcm-token`
4. Notifications are sent automatically on new messages — muted rooms are skipped, stale tokens are cleaned up

## Flutter Client

A ready-made Flutter package that connects to this backend is available at [`chat-app-flutter`](../chat-app-flutter). Drop it into any Flutter app with a few lines of code.

## Docker Compose

```yaml
# Starts MongoDB + the chat service
docker-compose up -d
```

Uploaded files are stored in a named Docker volume (`chat-uploads`) so they survive container restarts.

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a PR.

- Branch from `develop`, not `main`
- Open an issue first for large changes
- PRs are reviewed and merged into `develop` before reaching `main`

## License

[MIT](LICENSE)
