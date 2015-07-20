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

### List external dependencies at start, and internal ones at end

Good:
```
(ns stonecutter.namespace
  (:require [clauth.token :as cl-token]
            [net.cgrand.enlive-html :as html]
            [stonecutter.helper :as helper]
            [stonecutter.db.storage :as storage]))
```

Bad:
```
(ns stonecutter.namespace
  (:require
            [clauth.token :as cl-token]
            [stonecutter.helper :as helper]
            [net.cgrand.enlive-html :as html]
            [stonecutter.db.storage :as storage]))
```

## Code Structure

### Put route handler functions into controller namespaces


## Tests

### Create global defs for css selectors
