This change removes all logging statements that appear to log sensitive data.

```diff
- logger.info("User token: " + securityToken);
```
