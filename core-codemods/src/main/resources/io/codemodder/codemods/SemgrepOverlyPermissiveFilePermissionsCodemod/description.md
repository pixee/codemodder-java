This change...

Our changes look something like this:

```diff
  Set<PosixFilePermission> filePermissions = new HashSet<PosixFilePermission>();
- filePermissions.add(PosixFilePermission.OTHERS_WRITE);
+ filePermissions.add(PosixFilePermission.GROUP_WRITE);
  Files.setPosixFilePermissions(script,filePermissions);
```

Note: It's worth considering whether you could use a more restrictive permission than `GROUP_WRITE` here. For example, if the file is owned by the same user that's running the application, you could use `OWNER_WRITE` instead.
