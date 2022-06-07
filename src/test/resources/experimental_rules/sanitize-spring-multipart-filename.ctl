GIVEN METHOD_CALL getOriginalFilename WHERE
  name = getOriginalFilename
  arguments.size = 0
  type = org.springframework.web.multipart.MultipartFile
  enclosingStatement.code !=~ validate
  enclosingStatement.code !=~ toSimpleFileName

TRANSFORM
  RETURN org.pixee.security.SafeIO.toSimpleFileName($getUploadFilename)
