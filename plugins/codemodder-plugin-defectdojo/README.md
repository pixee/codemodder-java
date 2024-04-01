This plugin adds support for results discovered imported into DefectDojo. 

The plugin works by forwarding a JSON file to the codemods. The JSON file is expected to be the output of the [DefectDojo Findings API](https://demo.defectdojo.org/api/v2/oa3/swagger-ui/). For creating test cases, you may want to bring retrieve a single issue in that JSON. Here is a `curl` command to do just that:

```bash
$ curl -H "Authorization: Token $DD_KEY" "https://pixee-test.cloud.defectdojo.com/api/v2/findings/?product_name=<PRODUCT_NAME>" | jq . 
```

You can't unfortunately filter findings on the SCM repository, so you will have to filter the results manually in some other way.

DefectDojo offers many features, but its main relevance in this context is as a vulnerability aggregator. The findings it contains will originate from a variety of other scanner tools. Therefore, the codemods that target these findings should be an extremely thin layer over a codemod that targets the scanner tool directly. 
