# Coding Conventions

## Namespaces

### Prefer :as over :refer

Good:
```
(ns mynamespace
  (:require [first.dependency :as fd]))

(fd/do-thing)
```

Less good:
```
(ns mynamespace
  (:require [first.dependency :refer [do-thing]]))

(do-thing)
```

### Prefix clauth aliases with cl-

Good:
```
(ns mynamespace
  (:require [clauth.user :as cl-user]))
```

Bad:
```
(ns mynamespace
  (:require [clauth.user :as user]))
```
