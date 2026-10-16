import { useState, type FormEvent } from "react";
import { PageHeader } from "../components/ui/PageHeader";
import { useAuth } from "../auth/useAuth";
import { changePassword, requestPasswordChangeCode } from "../api/auth";

export function ProfilePage() {
  const { user, updateName } = useAuth();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [codeSent, setCodeSent] = useState(false);
  if (!user) return null;
  const email = user.email;

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await updateName(String(form.get("displayName")).trim());
      setNotice("Profile updated.");
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : "Could not update your profile.",
      );
    } finally {
      setBusy(false);
    }
  }

  async function sendCode() {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await requestPasswordChangeCode();
      setCodeSent(true);
      setNotice(`A verification code was sent to ${email}.`);
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : "Could not send a verification code.",
      );
    } finally {
      setBusy(false);
    }
  }

  async function updatePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const target = event.currentTarget;
    const form = new FormData(target);
    const verificationCode = String(form.get("verificationCode") ?? "").trim();
    const newPassword = String(form.get("newPassword") ?? "");
    const confirmPassword = String(form.get("confirmPassword") ?? "");
    setError("");
    setNotice("");
    if (newPassword !== confirmPassword) {
      setError("The new passwords do not match.");
      return;
    }
    setBusy(true);
    try {
      await changePassword(verificationCode, newPassword);
      target.reset();
      setCodeSent(false);
      setNotice("Password changed successfully.");
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : "Could not change your password.",
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="ACCOUNT"
        title="My profile"
        description="Manage your profile and account security."
      />
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      {notice && (
        <p className="notice" role="status">
          {notice}
        </p>
      )}
      <form
        className="workspace-form settings-form"
        onSubmit={(event) => void save(event)}
      >
        <label>
          Display name
          <input
            name="displayName"
            required
            maxLength={100}
            defaultValue={user.displayName}
          />
        </label>
        <label>
          Email address
          <input type="email" value={user.email} readOnly />
        </label>
        <p className="member-note">
          Email address is managed by your account and cannot be changed here.
        </p>
        <button className="primary-button compact" disabled={busy}>
          {busy ? "Saving…" : "Save profile"}
        </button>
      </form>
      <section className="detail-card profile-account-info">
        <h2>Change password</h2>
        <p className="subtle">
          We will send a one-time code to {user.email} before changing your
          password.
        </p>
        <form
          className="workspace-form settings-form"
          onSubmit={(event) => void updatePassword(event)}
        >
          <button
            type="button"
            className="secondary-button compact"
            disabled={busy}
            onClick={() => void sendCode()}
          >
            {codeSent ? "Resend verification code" : "Send verification code"}
          </button>
          <label>
            Verification code
            <input
              name="verificationCode"
              inputMode="numeric"
              autoComplete="one-time-code"
              pattern="[0-9]{6}"
              minLength={6}
              maxLength={6}
              required
              onChange={(event) => {
                event.currentTarget.value = event.currentTarget.value
                  .replace(/\D/g, "")
                  .slice(0, 6);
              }}
            />
          </label>
          <label>
            New password
            <input
              name="newPassword"
              type="password"
              autoComplete="new-password"
              minLength={12}
              required
            />
            <span className="field-hint">At least 12 characters.</span>
          </label>
          <label>
            Confirm new password
            <input
              name="confirmPassword"
              type="password"
              autoComplete="new-password"
              minLength={12}
              required
            />
          </label>
          <button
            className="primary-button compact"
            disabled={busy || !codeSent}
          >
            {busy ? "Updating…" : "Change password"}
          </button>
        </form>
      </section>
      <section className="detail-card profile-account-info">
        <h2>Account</h2>
        <dl className="detail-grid">
          <div>
            <dt>Member since</dt>
            <dd>{new Date(user.createdAt).toLocaleDateString()}</dd>
          </div>
        </dl>
      </section>
    </>
  );
}
