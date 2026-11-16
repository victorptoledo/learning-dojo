# A Terraform Module with Guard Rails

Write a reusable Terraform module: typed variables that reject bad input (including a rule
that depends on two variables), locals for naming and tags, one resource per region with
`for_each`, and outputs. Press **Enter** to create the kata folder and open it, edit the
`.tf` files and save. The dojo formats, validates and tests the module offline with
`terraform test`; no cloud account is needed.
