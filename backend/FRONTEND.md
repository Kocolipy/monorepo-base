# Front-end changes required

The backend now enforces CSRF protection, sends a Content-Security-Policy, and
expires sessions sooner. The first of those is a breaking change: every unsafe
request from the SPA will fail with `403` until the front end sends the token.

## 1. Send the CSRF token on every unsafe request

Read the `XSRF-TOKEN` cookie and echo its value in the `X-XSRF-TOKEN` header on
every `POST`, `PUT`, `PATCH`, and `DELETE`. Safe methods (`GET`, `HEAD`,
`OPTIONS`) need nothing.

The cookie is deliberately not `HttpOnly`, so JavaScript can read it:

```js
function csrfToken() {
  return document.cookie
    .split("; ")
    .find((entry) => entry.startsWith("XSRF-TOKEN="))
    ?.split("=")[1];
}

await fetch("/api/auth/login", {
  method: "POST",
  credentials: "same-origin",
  headers: {
    "Content-Type": "application/json",
    "X-XSRF-TOKEN": csrfToken(),
  },
  body: JSON.stringify({ username, password }),
});
```

Read the cookie at request time, inside the interceptor or request helper. Do
not capture it into a module-level constant or React state at start-up.

If the app uses **axios**, this is already its default behaviour for same-origin
requests (`xsrfCookieName: "XSRF-TOKEN"`, `xsrfHeaderName: "X-XSRF-TOKEN"`), so
confirm `withCredentials` is set and no custom `xsrf*` options override the
defaults. **Angular's** `HttpClient` does the same via `HttpClientXsrfModule`.

### The login request needs a token too

`POST /api/auth/login` is public but not exempt from CSRF: the check runs before
authentication. The token arrives with any earlier response, including anonymous
ones, so loading `index.html` or a `401` from `GET /api/auth/me` is enough to
seed the cookie. A cold start that posts to `/api/auth/login` as its very first
HTTP call will get `403` — make one safe request first if that is possible in the
current flow.

## 2. Expect the token to change on login and logout

Both endpoints return a replacement `XSRF-TOKEN` cookie:

- **Login** rotates it, so a token obtained while anonymous cannot be reused
  against the authenticated session.
- **Logout** replaces it as well, and the token it hands back is valid, so the
  next login can be submitted straight away.

The browser applies these cookies automatically. Nothing is required here *as
long as* rule 1 is followed and the token is read per request.

## 3. Treat 403 and 401 differently

| Status | Meaning | Reasonable response |
| --- | --- | --- |
| `401` | No authenticated session, or it expired | Redirect to login |
| `403` | CSRF token missing, stale, or mismatched | Retry once after a `GET`; do not log the user out |

Sending the browser to the login screen on a `403` will look like a random
logout, because the underlying cause is usually a missing header rather than an
ended session.

## 4. Logout no longer leaves a session cookie behind

`DELETE /api/auth/logout` now returns an expired session cookie. The browser
drops it on its own. Any front-end code that tried to clear `JSESSIONID`
manually can go.

## 5. Sessions now expire after 15 minutes of inactivity

The inactivity window dropped from 30 to 15 minutes, so idle tabs will start
seeing `401` sooner. If the app shows a session-expiry warning or a countdown,
update the interval it assumes.

## 6. Content-Security-Policy

Responses now carry:

```
default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline';
img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none';
base-uri 'self'; form-action 'self'; frame-ancestors 'none'
```

What this rules out:

- **Inline `<script>` and `eval`.** The current Vite build only emits external
  module scripts, so it is unaffected. Avoid plugins that inject inline script.
- **Third-party origins** for scripts, styles, fonts, images, and `fetch`/
  `XHR`/`WebSocket` targets. Anything loaded from a CDN or an external analytics
  or font host will be blocked. Self-host it, or ask for the directive to be
  widened for that specific origin.
- **Framing**: the app cannot be embedded, and cannot embed anything.

Inline **styles** are still allowed, because component libraries commonly inject
`<style>` elements at runtime. If the app does not rely on that, say so and
`'unsafe-inline'` can come out of `style-src`.

`Referrer-Policy: strict-origin-when-cross-origin` and a `Permissions-Policy`
denying geolocation, camera, microphone, payment, and USB are also sent. If a
feature legitimately needs one of those APIs, it needs a backend change.
