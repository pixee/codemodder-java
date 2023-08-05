This change removes an extra, expensive step commonly seen in Jackson (and other object deserialization) usage. 

Deserializing data from a stream is a one-step process, but sometimes developers will convert the stream to a string first. This is unnecessary and can be expensive for large payloads. This change removes that first step and it looks like this:

```diff
- String json = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
  ObjectMapper mapper = new ObjectMapper();
- Acme acme = mapper.readValue(json, Acme.class);
+ Acme acme = mapper.readValue(inputStream, Acme.class);
```
