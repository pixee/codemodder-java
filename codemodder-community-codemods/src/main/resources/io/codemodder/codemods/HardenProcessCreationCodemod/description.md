This change hardens all instances of [Runtime#exec()](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Runtime.html) to offer protection against attack.

Left unchecked, `Runtime#exec()` can execute any arbitrary system command. If an attacker can control part of the strings used to as program paths or arguments, they could execute arbitrary programs, install malware, and anything else they could do if they had a shell open on the application host.

Our change introduces a sandbox which protects the application:

```diff
+ import io.github.pixee.security.SystemCommand;
  ...
- Process p = Runtime.getRuntime().exec(command);
+ Process p = SystemCommand.runCommand(Runtime.getRuntime(), command);
```

The default restrictions applied are the following:
* **Prevent command chaining**. Many exploits work by injecting command separators and causing the shell to interpret a second, malicious command. The `SystemCommand#runCommand()` attempts to parse the given command, and throw a `SecurityException` if multiple commands are present.
* **Prevent arguments targeting sensitive files.** There is little reason for custom code to target sensitive system files like `/etc/passwd`, so the sandbox prevents arguments that point to these files that may be targets for exfiltration.

There are [more options for sandboxing](https://github.com/pixee/java-security-toolkit/blob/main/src/main/java/io/github/pixee/security/SystemCommand.java#L15) if you are interested in locking down system commands even more.
