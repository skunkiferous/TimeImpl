Time
====

This project tries to solve several time-related problems:

1. We would like to get the current time at a higher precision then milliseconds.
2. We would like to get the current time in UTC/GMT, so we don't have to deal with summer/winter clock changes.
3. We would like to be able to rely on the fact that the clock *never goes backward*.
4. We would like a lightweight task scheduling system based on the previously defined time.
5. We would like for all this to still work, in the presence of a bad (wrong time, wrong time-zone) system clock.
6. We would like for all this to still work, independent of the fact that the host gets synchronized or not.
7. Building on top of the accurate UTC time, we would like to get an accurate local time too. This does not depend on a correct local clock, but it does depend on a correctr time-zone. There is no reliable way to discover the local timezone.

This, as you can imagine, requires that we go out in the Internet, and query what the real time is, independent of what the host think it is. This all grew from the realisation, that a large part of our users have totally wrong system time, and we need accurate system time for our operation. So the use of this API relieves the user from the burden of making sure the local system time is correct. We do expect that the user will at least get their time-zone right, if they want accurate local time.

