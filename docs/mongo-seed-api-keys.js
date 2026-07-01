const keys = [
  {
    keyValue: 'SUPER-SECRET-DEV-KEY-123',
    owner: 'dev',
    active: true,
    createdAt: new Date().toISOString(),
  },
  {
    keyValue: 'EXPIRED-HACKER-KEY-999',
    owner: 'expired',
    active: false,
    createdAt: new Date().toISOString(),
  },
];

keys.forEach((key) => {
  db.api_keys.updateOne({ keyValue: key.keyValue }, { $set: key }, { upsert: true });
});

print('Seeded api_keys collection.');
