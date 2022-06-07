# Code Transformation Language (CTL)

In order to make it easy to write, test and organize rules, we have created a simple language called Code Transformation Language (CTL). 

# CTL Script Structure
There are 3 sections to each CTL script.

## Preamble
The purposes of the preamble are to document the script and, if needed, setup any exotic requirements. The following
properties are available to be set:

 * `ruleId` (required) the ID of the rule, as a string, e.g., `pixee:java/harden-xstream`
 * `version` (optional) the current version of the rule, e.g., `1.6` or `1.5-BETA`
 * `tags` (optional) a set of user-defined tags to associate with this rule or its results

Example with the optional `version` and `tags` fields present:
```
ruleId = pixee:java/harden-xstream
version = 1.6
tags = [xml,xstream,paranoid]
```

## Matching Predicates
This is a selection of predicates whose job it is to select the matching AST nodes we want to transform.

```
GIVEN [NODE TYPE] WHERE
   [PREDICATE FOR THE NODE TYPE 1]
   [PREDICATE FOR THE NODE TYPE 2]
   ...
   [PREDICATE FOR THE NODE TYPE n]
```

Possible NODE TYPEs include... 

## Transformation Procedure
This is a procedural block of code that performs the transformation.

```
TRANSFORM
  
```