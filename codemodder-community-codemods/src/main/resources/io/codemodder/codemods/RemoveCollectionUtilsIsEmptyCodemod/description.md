This change removes CollectionUtils.isEmpty() calls in order to remove the dependency on Apache Commons Collections.

```dif
- return CollectionUtils.isEmpty(list);
+ return list == null || list.isEmpty();
```
This library has already offered risk by...
