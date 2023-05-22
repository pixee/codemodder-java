The `web.xml` specification offers a way to protect certain parts of your URL space. Unfortunately, it doesn't work the way people think it does, developers who are trying to enhance their security often end up accidentally exposing those parts they were trying to protect.

Consider the following `web.xml`, which is trying to restrict the `/admin/*` space to only those with the `admin` role:
```xml
<security-constraint>
  <web-resource-collection>
    <url-pattern>/admin/*</url-pattern>
    <http-method>GET</http-method>
    <http-method>POST</http-method>
  </web-resource-collection>
  <auth-constraint>
    <role-name>admin</role-name>
  </auth-constraint>
</security-constraint>
```

This protection works as expected with one regrettable caveat. Notice that the `GET` and `POST` methods are specifically listed. Developers often specify methods like this because they want to further control what types of methods can access the given resource.

Unfortunately, the logic of the mechanism is surprising. Specifying method(s) means if a user issues another HTTP method besides the ones listed, like in this case, `HEAD`, `PUT`, or even a nonsense verb like `JEFF`, the protection will not be deemed to apply to the given `<security-constraint>`, and the requester will be granted unfettered access.

Our change is simple: any place we see `<http-method>` listed in a `<security-constraint>`, we remove it:

```diff
  <security-constraint>
    <auth-constraint>
      <role-name>admin</role-name>
    </auth-constraint>
    <web-resource-collection>
      <url-pattern>/admin/*</url-pattern>
-      <http-method>GET</http-method>
-      <http-method>POST</http-method>
    </web-resource-collection>
  </security-constraint>
```

Taking out all the `<http-method>` entries tells the server that this protection must be enforced for all methods, which is almost always the intent of the developer.
