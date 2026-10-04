import { describe, expect, it } from "vitest";

import { DecodeError } from "@/lib/decode";

import {
  decodeConnector,
  decodeConnectors,
  decodeGroupRow,
  decodeGroupRows,
  decodeIssuedToken,
  decodeUserRow,
  decodeUserRows,
} from "./accounts-api";

/**
 * Each wire type as the backend serialises it: every field present, `null`s
 * included, so a test that drops or retypes one field isolates that field.
 */
const DIRECT_GROUP = { id: "g-1", displayName: "Engineering" };

const USER_ROW = {
  id: "u-1",
  userName: "grace",
  displayName: null,
  admin: false,
  bootstrapAdmin: false,
  active: true,
  locked: false,
  hasPassword: true,
  passwordChangeRequired: false,
  lastAuthenticatedAt: null,
  createdAt: "2026-01-02T03:04:05Z",
  groups: [DIRECT_GROUP],
};

const GROUP_ROW = { id: "g-1", displayName: "Engineering", memberCount: 2, adminGroup: false };

const CONNECTOR_TOKEN = {
  id: "t-1",
  permissions: ["group:read", "group:write", "user:read", "user:write"],
  issuedAt: "2026-01-02T03:04:05Z",
  expiresAt: "2026-04-02T03:04:05Z",
  originalExpiresAt: "2026-04-02T03:04:05Z",
  revokedAt: null,
  active: true,
};

const CONNECTOR = {
  id: "c-1",
  displayName: "Okta",
  createdAt: "2026-01-02T03:04:05Z",
  tokens: [CONNECTOR_TOKEN],
};

const ISSUED_TOKEN = {
  connectorId: "c-1",
  tokenId: "t-1",
  permissions: ["user:read"],
  issuedAt: "2026-01-02T03:04:05Z",
  expiresAt: "2026-04-02T03:04:05Z",
  presentedValue: "scim_plaintext",
};

const without = (body: object, key: string) =>
  Object.fromEntries(Object.entries(body).filter(([field]) => field !== key));

const refusal = (run: () => unknown, message: string) =>
  expect(run).toThrow(new DecodeError(message));

/**
 * How one field is checked: a value of the wrong type for it, what the refusal
 * says that value is not, and what a missing field is not (the same, except a
 * closed set, which is first read as a string).
 */
type FieldCheck = [wrong: unknown, expected: string, whenMissing?: string];

/**
 * One decoder's contract: the four cases the issue names, applied to every
 * field so no field's check can be dropped unnoticed. Each refusal names the
 * field that failed, so a case cannot pass on some other field's failure.
 */
function describeDecoder(
  name: string,
  decode: (value: unknown) => unknown,
  valid: Record<string, unknown>,
  fields: Record<string, FieldCheck>,
) {
  describe(name, () => {
    it("decodes a valid body to the same value", () => {
      expect(decode(valid)).toStrictEqual(valid);
    });

    it("drops nothing the wire did not send and adds nothing", () => {
      expect(Object.keys(decode({ ...valid, extra: "ignored" }) as object).sort()).toStrictEqual(
        Object.keys(valid).sort(),
      );
    });

    it("checks every field the type has", () => {
      expect(Object.keys(fields).sort()).toStrictEqual(Object.keys(valid).sort());
    });

    it.each(Object.entries(fields))("refuses a body missing %s", (key, [, expected, missing]) => {
      refusal(() => decode(without(valid, key)), `${name}.${key} is not ${missing ?? expected}`);
    });

    it.each(Object.entries(fields))("refuses a wrong-typed %s", (key, [value, expected]) => {
      refusal(() => decode({ ...valid, [key]: value }), `${name}.${key} is not ${expected}`);
    });

    it.each([
      ["an array", [valid]],
      ["null", null],
      ["a string", "body"],
    ])("refuses %s as a body", (_, value) => {
      refusal(() => decode(value), `${name} is not an object`);
    });
  });
}

const STRING: FieldCheck = [7, "a string"];
const BOOLEAN: FieldCheck = ["true", "a boolean"];

describeDecoder("UserRow", decodeUserRow, USER_ROW, {
  id: STRING,
  userName: STRING,
  displayName: [false, "a string"],
  admin: BOOLEAN,
  bootstrapAdmin: BOOLEAN,
  active: BOOLEAN,
  locked: BOOLEAN,
  hasPassword: BOOLEAN,
  passwordChangeRequired: BOOLEAN,
  lastAuthenticatedAt: [0, "a string"],
  createdAt: [null, "a string"],
  groups: [DIRECT_GROUP, "an array"],
});

describeDecoder("GroupRow", decodeGroupRow, GROUP_ROW, {
  id: STRING,
  displayName: [null, "a string"],
  memberCount: ["2", "an integer"],
  adminGroup: BOOLEAN,
});

describeDecoder("Connector", decodeConnector, CONNECTOR, {
  id: STRING,
  displayName: STRING,
  createdAt: STRING,
  tokens: [null, "an array"],
});

describeDecoder("IssuedToken", decodeIssuedToken, ISSUED_TOKEN, {
  connectorId: STRING,
  tokenId: STRING,
  permissions: [null, "an array"],
  issuedAt: STRING,
  expiresAt: STRING,
  presentedValue: [null, "a string"],
});

// The two element types reached only through a parent: their contract is
// exercised through it, one element at a time.

describeDecoder(
  "DirectGroup",
  (value) => decodeUserRow({ ...USER_ROW, groups: [value] }).groups[0],
  DIRECT_GROUP,
  {
    id: STRING,
    displayName: [null, "a string"],
  },
);

describeDecoder(
  "ConnectorToken",
  (value) => decodeConnector({ ...CONNECTOR, tokens: [value] }).tokens[0],
  CONNECTOR_TOKEN,
  {
    id: STRING,
    permissions: ["user:read", "an array"],
    issuedAt: STRING,
    expiresAt: STRING,
    originalExpiresAt: [undefined, "a string"],
    revokedAt: [true, "a string"],
    active: BOOLEAN,
  },
);

/** Each listing decoder, the row type it reads, and one valid row of it. */
const LISTINGS: [list: string, decode: (value: unknown) => unknown, row: object, type: string][] = [
  ["UserRow[]", decodeUserRows, USER_ROW, "UserRow"],
  ["GroupRow[]", decodeGroupRows, GROUP_ROW, "GroupRow"],
  ["Connector[]", decodeConnectors, CONNECTOR, "Connector"],
];

describe("the listing decoders", () => {
  it.each(LISTINGS)("%s decodes every row", (_, decode, row) => {
    expect(decode([row, row])).toStrictEqual([row, row]);
    expect(decode([])).toStrictEqual([]);
  });

  it.each(LISTINGS)("%s refuses a listing with one malformed row", (_, decode, row, type) => {
    refusal(() => decode([row, without(row, "id")]), `${type}.id is not a string`);
    refusal(() => decode([row, 7]), `${type} is not an object`);
  });

  it.each(LISTINGS)("%s refuses a body that is not a list", (list, decode, row) => {
    refusal(() => decode(row), `${list} is not an array`);
    refusal(() => decode(null), `${list} is not an array`);
  });
});

describe("token permissions", () => {
  it("decodes an empty list, which a token from before Permissions carries", () => {
    expect(
      decodeConnector({ ...CONNECTOR, tokens: [{ ...CONNECTOR_TOKEN, permissions: [] }] }).tokens[0]
        .permissions,
    ).toStrictEqual([]);
  });

  it.each([["audit:read"], ["READ_WRITE"], ["User:Read"], [7], [null]])(
    "refuses %s, which no token can carry",
    (value) => {
      refusal(
        () => decodeIssuedToken({ ...ISSUED_TOKEN, permissions: ["user:read", value] }),
        "token permissions hold an unknown value",
      );
      refusal(
        () =>
          decodeConnector({ ...CONNECTOR, tokens: [{ ...CONNECTOR_TOKEN, permissions: [value] }] }),
        "token permissions hold an unknown value",
      );
    },
  );
});
