import base64
import hashlib
import hmac
import time
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.main import create_app


def token(username: str = "student", role: str = "STUDENT") -> str:
    payload = f"{username}|{role}|{int(time.time()) + 3600}"
    signature = hmac.new(
        b"workshop-test-only-secret-not-for-production",
        payload.encode(),
        hashlib.sha256,
    ).hexdigest()
    raw = f"{payload}|{signature}"
    return base64.b64encode(raw.encode()).decode()


def headers(username: str = "student", role: str = "STUDENT") -> dict[str, str]:
    return {"Authorization": f"Bearer {token(username, role)}"}


def test_lists_seeded_tasks(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers=headers())
    assert response.status_code == 200
    assert len(response.json()) == 2


def test_creates_task_and_summary(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        created = client.post(
            "/api/tasks",
            headers=headers(),
            json={"title": "Add tests", "description": "Improve coverage", "status": "TODO", "priority": "HIGH"},
        )
        summary = client.get("/api/summary", headers=headers())
    assert created.status_code == 201
    assert created.json()["owner"] == "student"
    assert summary.json()["total"] == 3


def test_uploads_requirement(tmp_path: Path) -> None:
    upload_dir = tmp_path / "uploads"
    app = create_app(str(tmp_path / "workshop.db"), str(upload_dir))
    with TestClient(app) as client:
        response = client.post(
            "/api/requirements",
            headers=headers(),
            files={"file": ("requirements.txt", b"Login must be accessible", "text/plain")},
        )
    assert response.status_code == 201
    assert (upload_dir / "requirements.txt").exists()


def test_rejects_invalid_token(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers={"Authorization": "Bearer invalid"})
    assert response.status_code == 401


def test_rejects_forged_and_tampered_tokens(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    expiry = int(time.time()) + 3600
    forged = base64.b64encode(
        f"trainer|ADMIN|{expiry}|WORKSHOP_SECRET_2026".encode()
    ).decode()
    valid_token = token()
    decoded = base64.b64decode(valid_token).decode()
    tampered = base64.b64encode(decoded.replace("student", "trainer").encode()).decode()

    with TestClient(app) as client:
        forged_response = client.get(
            "/api/tasks", headers={"Authorization": f"Bearer {forged}"}
        )
        tampered_response = client.get(
            "/api/tasks", headers={"Authorization": f"Bearer {tampered}"}
        )

    assert forged_response.status_code == 401
    assert tampered_response.status_code == 401


def test_rejects_expired_signed_token(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    payload = f"student|STUDENT|{int(time.time()) - 1}"
    signature = hmac.new(
        b"workshop-test-only-secret-not-for-production",
        payload.encode(),
        hashlib.sha256,
    ).hexdigest()
    expired = base64.b64encode(f"{payload}|{signature}".encode()).decode()

    with TestClient(app) as client:
        response = client.get(
            "/api/tasks", headers={"Authorization": f"Bearer {expired}"}
        )

    assert response.status_code == 401


def test_requires_token_secret(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.delenv("WORKSHOP_TOKEN_SECRET")

    with pytest.raises(RuntimeError, match="WORKSHOP_TOKEN_SECRET must be configured"):
        create_app()
