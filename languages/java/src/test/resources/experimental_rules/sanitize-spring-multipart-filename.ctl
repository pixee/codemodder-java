rule pixee:java/sanitize-apache-spring-filename

match
  InstanceMethodCall $c {
    type = org.springframework.web.multipart.MultipartFile
    name = getOriginalFilename
    args = []
  }

where
  !hasUpstream(
    $c,
    StaticMethodCall {
      type = io.pixee.security.SafeIO
      name = toSimpleFileName
      args = [$c]
    }
  )

require dependency io.openpixee:java-security-toolkit:1.0.0
require import org.openpixee.security.Filenames

insert into data flow after $c
  StaticMethodCall {
    type = io.openpixee.security.Filenames
    name = toSimpleFileName
    args = [$c]
  }