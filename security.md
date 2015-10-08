Security notes for Stonecutter SSO
=====

In this document, we briefly address the security of the Stonecutter
SSO provider, and provide details of how it mitigates against common
attacks.  This document has been derived largely from the "security
considerations" sections of the OAuth 2.0 Core and OpenId Connect
specifications (<insert links>), with the understanding that
Stonecutter's intended use-case is as an SSO provider, not a complete
OAuth 2.0 Authorisation service (thus, some concerns around resource
protection are of less relevance).

As a single sign-on provider, Stonecutter's essential task is
authentication --- that is, verifying a user's identity, and proving
it to the client applications registered with Stonecutter.

OAuth 2.0
=====

Stonecutter does not implement the complete OAuth 2.0 protocol; thus
some of the concerns indicated in the spec are not relevant.  In particular:

- Stonecutter does not support the 'implicit' flow

- Stonecutter is focussed on 'confidential' clients --- that is,
  clients that can ensure the confidentiality / security of their
  client credentials.  Note that this precludes using Stonecutter for
  native mobile applications or other applications designed to execute
  on user-controlled infrastructure.

- As an SSO provider, Stonecutter is less concerned with providing
  client applications access to protected resources; rather, it
  focuses on proving a user's identity to a client.  Stonecutter may
  at some point act as both a resource provider and an authorisation
  provider, in order to serve user profile information.

Some basic security considerations:

- Stonecutter requires all client and user interaction with the
  Stonecutter auth server to take place over a secure channel, using
  TLS.  Client applications should validate the TLS certificate of
  their Stonecutter instance when establishing the channel.

- All form posts on the Stonecutter auth server are protected against
  CSRF attacks via tokens bound to a specific user session (the
  Synchroniser token pattern).

# Specific security threats and their mitigation

## Cross-site request forgery

(See [Section 10.12 of the RFC 6749](https://tools.ietf.org/html/rfc6749#section-10.12))

Stonecutter clients must implement CSRF protection for their
authorisation redirect endpoints.

- Require clients to register full redirect uri; Stonecutter will not
  redirect the user if the redirect uri does not match that registered
  against the client-id exactly.

- Clients should make use of the 'state' parameter to ensure the user
  session following the redirect is the same user session as that for
  which the authorisation code was issued.

This is particularly relevant for clients allowing user authentication
with Stonecutter, along with other SSO providers (i.e. facebook or
twitter).  see e.g.:

- http://homakov.blogspot.co.uk/2012/07/saferweb-most-common-oauth2.html

- https://www.online24.nl/blog/common-csrf-vulnerability-in-oauth2-implementations/

## Open redirects

- Stonecutter currently requires clients to register their base url,
  and will refuse to issue redirects to urls that don't match these
  values --- so, for example, if a client has registered
  ```https://client.com/```, stonecutter will refuse to redirect a
  user authenticating with that client to
  ```https://some.other.client.com/process_auth_code```, but will
  accept redirects to both ```https://client.com/process_auth_code```
  and ```https://client.com/some_other_endpoint```.

- While this protects against redirects to arbitrary uris from
  Stonecutter directly, a client with an open redirect url could be
  co-opted to perform arbitrary redirects.

- /TODO/ - Require clients to register the entire redirect uri with
  Stonecutter, and check the full uri before redirecting to
  mitigate. (see
  [section 10.6 of RFC 6749](https://tools.ietf.org/html/rfc6749#section-10.6))
