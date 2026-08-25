const test = require('node:test');
const assert = require('node:assert/strict');
const {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} = require('@firebase/rules-unit-testing');
const { doc, getDoc, setDoc, deleteDoc, writeBatch, Timestamp, serverTimestamp } = require('firebase/firestore');
const fs = require('node:fs');
const path = require('node:path');

const PROJECT_ID = 'demo-elwataniatv-rules';
const RULES_PATH = path.resolve(__dirname, '../../admin/firestore.rules');
const PRODUCTION_RULES = fs.readFileSync(RULES_PATH, 'utf8');
// The current Firestore Emulator context does not expose request.app.
// Substitute only this helper for local tests; production rules remain strict.
const EMULATOR_RULES = PRODUCTION_RULES.replace(
  /function hasAppCheck\(\) \{\s*return request\.app != null;\s*\}/,
  'function hasAppCheck() { return true; }',
);
assert.match(PRODUCTION_RULES, /return request\.app != null;/);
assert.notEqual(EMULATOR_RULES, PRODUCTION_RULES);

let testEnv;

const userAuth = { uid: 'user-1', token: { admin: false } };
const otherUserAuth = { uid: 'user-2', token: { admin: false } };
const adminAuth = { uid: 'admin-1', token: { admin: true, role: 'admin' } };
const editorAuth = { uid: 'editor-1', token: { admin: false, role: 'editor' } };
const moderatorAuth = { uid: 'moderator-1', token: { admin: false, role: 'moderator' } };

function firestoreFor(auth) {
  return testEnv.authenticatedContext(auth.uid, auth.token).firestore();
}

function unauthenticatedFirestore() {
  return testEnv.unauthenticatedContext().firestore();
}

async function createComment(auth, path, data) {
  const db = firestoreFor(auth);
  const batch = writeBatch(db);
  batch.set(doc(db, path), data);
  batch.set(doc(db, `users/${auth.uid}`), {
    lastCommentAt: serverTimestamp(),
  }, { merge: true });
  return batch.commit();
}

function baseDevice(uid = 'user-1') {
  return {
    lastSeen: Timestamp.fromMillis(1_700_000_000_000),
    updatedAt: Timestamp.fromMillis(1_700_000_000_000),
    userUid: uid,
    installationId: 'install-12345678',
  };
}

function baseComment(uid = 'user-1') {
  return {
    author: 'Viewer',
    text: 'A valid authenticated comment',
    deviceId: 'device-12345678',
    userUid: uid,
    parentId: '',
    createdAt: serverTimestamp(),
    reactions: {},
  };
}

function basePushToken(uid = 'user-1') {
  return {
    deviceId: 'device-12345678',
    userUid: uid,
    token: 'fcm-token-that-is-at-least-20-chars',
    platform: 'android',
    savedAt: Timestamp.fromMillis(1_700_000_000_000),
  };
}

test.before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: EMULATOR_RULES,
    },
  });
});

test.after(async () => {
  if (testEnv) await testEnv.cleanup();
});

test('public read is allowed for a content document', async () => {
  await assertSucceeds(getDoc(doc(unauthenticatedFirestore(), 'streams/main')));
});

test('satellite frequencies are publicly readable', async () => {
  await assertSucceeds(getDoc(doc(unauthenticatedFirestore(), 'satellite_frequencies/nilesat')));
});

test('ordinary users cannot write satellite frequencies', async () => {
  await assertFails(setDoc(doc(firestoreFor(userAuth), 'satellite_frequencies/nilesat'), {
    satelliteName: 'Nilesat',
    frequencyMhz: 10922,
    symbolRate: 27500,
    isActive: true,
  }));
});

test('an editor can manage editorial content but not protected config', async () => {
  const db = firestoreFor(editorAuth);
  await assertSucceeds(setDoc(doc(db, 'archive/editor-program'), {
    title: 'Editorial program',
    isActive: true,
  }));
  await assertFails(setDoc(doc(db, 'config/editor-attempt'), {
    appVersion: '8.1.3',
  }));
});

test('a moderator can remove comments but cannot manage editorial content', async () => {
  const db = firestoreFor(moderatorAuth);
  await assertSucceeds(deleteDoc(doc(db, 'programs/program-1/comments/reported-comment')));
  await assertFails(setDoc(doc(db, 'archive/moderator-attempt'), {
    title: 'Moderator attempt',
  }));
  await assertFails(setDoc(doc(db, 'config/moderator-attempt'), {
    appVersion: '8.1.3',
  }));
});

test('an admin can create satellite frequencies', async () => {
  await assertSucceeds(setDoc(doc(firestoreFor(adminAuth), 'satellite_frequencies/nilesat'), {
    satelliteName: 'Nilesat',
    orbitalPosition: '7.0W',
    frequencyMhz: 10922,
    polarization: 'V',
    symbolRate: 27500,
    fec: '7/8',
    isActive: true,
    order: 1,
  }));
});

test('ordinary users cannot write likes, live reactions, or comment reactions', async () => {
  const db = firestoreFor(userAuth);
  await assertFails(setDoc(doc(db, 'likes/program-1'), { likes: 1 }));
  await assertFails(setDoc(doc(db, 'live/reactions'), { '👍': 1 }));
  await assertFails(setDoc(doc(db, 'programs/program-1/comments/comment-1/reactions/user-1'), { like: 1 }));
});

test('an admin can write protected aggregate and reaction documents', async () => {
  const db = firestoreFor(adminAuth);
  await assertSucceeds(setDoc(doc(db, 'likes/program-1'), { likes: 1 }));
  await assertSucceeds(setDoc(doc(db, 'live/reactions'), { '👍': 1 }));
  await assertSucceeds(setDoc(doc(db, 'programs/program-1/comments/comment-1/reactions/user-1'), { like: 1 }));
});

test('devices can be created only for the authenticated owner', async () => {
  await assertSucceeds(setDoc(doc(firestoreFor(userAuth), 'devices/user-1'), baseDevice()));
  await assertFails(setDoc(doc(firestoreFor(userAuth), 'devices/user-2'), baseDevice('user-2')));
  await assertFails(setDoc(doc(firestoreFor(otherUserAuth), 'devices/user-1'), baseDevice('user-1')));
});

test('device heartbeat accepts bounded app metadata', async () => {
  await assertSucceeds(setDoc(doc(firestoreFor(userAuth), 'devices/user-1'), {
    ...baseDevice(),
    appVersion: '8.0.0',
    platform: 'android',
  }));
});

test('device heartbeat rejects invalid app metadata', async () => {
  await assertFails(setDoc(doc(firestoreFor(userAuth), 'devices/user-1'), {
    ...baseDevice(),
    appVersion: '8.0.0',
    platform: 'ios',
  }));
  await assertFails(setDoc(doc(firestoreFor(userAuth), 'devices/user-1'), {
    ...baseDevice(),
    appVersion: 'x'.repeat(33),
    platform: 'android',
  }));
});

test('push_tokens accepts an optional bounded categories list', async () => {
  await assertSucceeds(setDoc(doc(firestoreFor(userAuth), 'push_tokens/user-1'), {
    ...basePushToken(),
    categories: ['all', 'sports'],
  }));
  await assertFails(setDoc(doc(firestoreFor(userAuth), 'push_tokens/user-1'), {
    ...basePushToken(),
    categories: Array.from({ length: 17 }, (_, index) => `category-${index}`),
  }));
});

test('push_tokens rejects an extra field', async () => {
  await assertFails(setDoc(doc(firestoreFor(userAuth), 'push_tokens/user-1'), {
    ...basePushToken(),
    extraField: 'must be rejected',
  }));
});

test('an authenticated user can create a valid comment', async () => {
  await assertSucceeds(createComment(
    userAuth,
    'programs/program-1/comments/comment-1',
    baseComment(),
  ));
});

test('an unauthenticated user cannot create a comment', async () => {
  await assertFails(setDoc(
    doc(unauthenticatedFirestore(), 'programs/program-1/comments/comment-unauthenticated'),
    baseComment(),
  ));
});

test('comment creation rejects unknown reaction keys', async () => {
  await assertFails(setDoc(
    doc(firestoreFor(userAuth), 'programs/program-1/comments/comment-unknown-reaction'),
    { ...baseComment(), reactions: { like: 1, angry: 1 } },
  ));
});

test('comment creation rejects negative reaction counters', async () => {
  await assertFails(setDoc(
    doc(firestoreFor(userAuth), 'programs/program-1/comments/comment-negative-reaction'),
    { ...baseComment(), reactions: { like: -1 } },
  ));
});

test('comment creation rejects unreasonably large reaction counters', async () => {
  await assertFails(setDoc(
    doc(firestoreFor(userAuth), 'programs/program-1/comments/comment-large-reaction'),
    { ...baseComment(), reactions: { love: 100001 } },
  ));
});

test('comment creation rejects non-numeric reaction counters', async () => {
  await assertFails(setDoc(
    doc(firestoreFor(userAuth), 'programs/program-1/comments/comment-string-reaction'),
    { ...baseComment(), reactions: { wow: '1' } },
  ));
});

test('comment creation accepts bounded known reaction counters', async () => {
  const reactionUser = { uid: 'user-reactions', token: { admin: false } };
  await assertSucceeds(createComment(
    reactionUser,
    'programs/program-1/comments/comment-valid-reactions',
    { ...baseComment(reactionUser.uid), reactions: { like: 1, love: 0, laugh: 2 } },
  ));
});

test('comment creation rejects any client-supplied createdAt (must be a server timestamp)', async () => {
  // A client timestamp (backdated or not) never equals request.time.
  await assertFails(setDoc(
    doc(firestoreFor(userAuth), 'programs/program-1/comments/comment-client-time'),
    { ...baseComment(), createdAt: Timestamp.now() },
  ));
});

test('a user can store lastCommentAt on their own doc only as a server timestamp', async () => {
  const userDb = firestoreFor(otherUserAuth);
  await assertSucceeds(setDoc(
    doc(userDb, 'users/user-2'),
    { lastCommentAt: serverTimestamp() },
  ));
  await assertFails(setDoc(
    doc(userDb, 'users/user-2'),
    { lastCommentAt: Timestamp.fromMillis(Date.now() - 60 * 1000) },
  ));
});

test('comment creation rejects more than one link', async () => {
  await assertFails(setDoc(
    doc(firestoreFor(userAuth), 'programs/program-1/comments/comment-two-links'),
    { ...baseComment(), text: 'انظر https://a.test و https://b.test' },
  ));
});

test('comment creation allows a single link', async () => {
  const linkUser = { uid: 'user-link', token: { admin: false } };
  await assertSucceeds(createComment(
    linkUser,
    'programs/program-1/comments/comment-one-link',
    { ...baseComment(linkUser.uid), text: 'الموقع الرسمي https://elwataniatv.dz' },
  ));
});

test('comment creation rejects text longer than 500 characters', async () => {
  await assertFails(setDoc(
    doc(firestoreFor(userAuth), 'programs/program-1/comments/comment-too-long'),
    { ...baseComment(), text: 'a'.repeat(501) },
  ));
});

test('a user cannot comment twice within the 30s posting interval', async () => {
  // Seed user-2's lastCommentAt with rules bypassed (adminContext is not
  // available in rules-unit-testing v3; withSecurityRulesDisabled is).
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'users/user-2'), {
      lastCommentAt: Timestamp.fromMillis(Date.now() - 5 * 1000),
    });
  });
  await assertFails(createComment(
    otherUserAuth,
    'programs/program-1/comments/comment-rate-limited',
    { ...baseComment('user-2'), author: 'Viewer 2', deviceId: 'device-2-1234567' },
  ));
});

test('a user can comment once the 30s posting interval has passed', async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'users/user-2'), {
      lastCommentAt: Timestamp.fromMillis(Date.now() - 120 * 1000),
    });
  });
  await assertSucceeds(createComment(
    otherUserAuth,
    'programs/program-1/comments/comment-rate-allowed',
    { ...baseComment('user-2'), author: 'Viewer 2', deviceId: 'device-2-1234567' },
  ));
});

test('unknown paths are denied', async () => {
  await assertFails(setDoc(doc(unauthenticatedFirestore(), 'unknown/path'), { value: true }));
  await assertFails(getDoc(doc(unauthenticatedFirestore(), 'unknown/path')));
});
