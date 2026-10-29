DCO
===

All code submitted to this repository needs a [Developer Certificate of Origin](http://developercertificate.org/)
(DCO) sign-off, as for [Besu](https://github.com/besu-eth/besu). The `DCO` check on pull requests
enforces it.

The signoff needs to be using your legal name, not a pseudonym. Git has a built-in mechanism to
allow this with the `-s` or `--signoff` argument to `git commit` command, providing your
`user.name` and `user.email` have been setup correctly.

TL;DR:

If you don't want to break the DCO check, ensure all your commits have signoff.

`git config user.name "FIRST_NAME LAST_NAME"`
`git config user.email "MY_NAME@example.com"`

If you use GitHub web UI for commits, also ensure you have your email address set as public in your
GitHub profile. This avoids issues with XYZ@users.noreply.github.com placeholder emails.

You can also set up a git alias, for example `git config alias.c 'commit -s'`.

More info and what to do if DCO is failing on your PR in the
[Working with DCO](https://github.com/besu-eth/besu/wiki/Contributing-Working-with-DCO) page of the
Besu wiki. The `DCO` check requires a `Signed-off-by` line that matches the commit's author or
committer, name and email.
