# Catalog Resources

The first redesign most APIs need: a catalog written as remote procedure calls over POST
(`/getProducts`, `/deleteProduct`), where every answer is `200 OK` and failure hides in an `ok`
flag. You turn it into two resources, the collection `/products` and the item
`/products/{id}`, and let the HTTP method say what happens and the status code say how it went.
Press **Enter** to create the kata folder and open it.
