# Module Boundaries

A modular monolith keeps one deployable and one database but splits the code into modules that
talk only through **published interfaces**, so each module can change its insides alone, and
could one day become a service. That promise holds only while nobody takes a shortcut. Here
three shortcuts already exist. You will write the boundary rule as a fitness function, then close
them. Press **Enter** to create the kata folder and open it.
