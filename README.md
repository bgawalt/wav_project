# WAVProject

This project takes two raw ingredients:

1. A long snippet of audio (.WAV format)
2. A shorter snippet of audio (also .WAV format)

and layers repeated echoes of (2), scaled and shifted,
to try and approximate (1).  Along the way, it writes
some of these successive approximations out to their
own .WAV files.

Let's call (1) the *target* audio, and (2) the *basis*
audio.

Here's a fantastic example of how lots and lots of
repeated versions of the same basis sound (one duck honk)
can smear together to approximate something quite
different (human-like screams):

[![IMAGE ALT TEXT](http://img.youtube.com/vi/nHc288IPFzk/0.jpg)](http://www.youtube.com/watch?v=nHc288IPFzk "Duck Army")

This project is that, just you get to also guide the smearing
of the repeated basis audio towards some target audio
of your choosing.

Right now, if you provide a WAV file with multiple channels,
e.g., in stereo, for either the target or the basis, it's
compressed down to a single mono channel signal.

## How to make a WAVProject

From this directory, run this command:

```
sbt/sbt 'runMain com.gawalt.wav_project.WavProject 
 path/to/long_target_audio.wav
 path/to/short_basis_audio.wav
 path/to/output_filename_base'
```

This will perform an approximation of the target audio by
layering scaled and shifted echoes of the basis audio.
The approximation is computed iteratively, one basis echo
per iteration.  

Unfortunately, right now that takes like two minutes per
iteration.  I might be able to speed this up?

## How is it implemented?

The approximation being calculated is a multivariate
linear regression, fit by coordinate descent.

Suppose the target WAV file is `N` samples long, and the
basis WAV is `K` samples.  The approximation we want to
produce should also be `N` samples long.  We can represent
the target as an array of doubles of length `N`, the basis
as an array of length `K`

We start with an `N`-length vector, full of zeros, as our
approximation.  The first `N - K + 1` elements of that
approximation vector are all "starting places" where we
could drop a scaled version of the `K`-length basis without
the basis overrunning the end of the approximation array.

Let's define the *residual* array as the difference of
target and approximation: `resid[i] = target[i] - approx[i]`
for `i = 0, ..., N - 1`.  Let's also define the sequence of
starting places, `s = 0, 1, ... N - K`.

Given those definitions, let's run this loop until we're bored:

1. For each starting place `s`, calculate the optimal amount
   to scale the basis vector so that it resembles the residual
   snippet running from `resid[s]` to `resid[s + K - 1]`.
   We want to pick a scaling factor `a` such that
   the sum of `(a*basis[j] - residual[s + j])^2` for
   `j = 0, ..., K - 1` is minimized.  It's easy to calculate
   a closed form answer to this -- it's essentially *vector
   projection* of that segment of the residual onto the basis.
2. Finds the starting position for which that squared-sum was
   smallest.  Call that position `s'` and its associated
   optimal scaling factor `a'`.
3. Update the approximation by adding in the optimally
   scaled basis function shifted to the starting place:
   `approx[s' + j] += a'*basis[j]` for `j = 0, ..., K - 1`.
4. Recalculate the residual from element `resid[s']` to
   `resid[s' + K - 1]` and repeat.

It's fun to solve this using coordinate descent, so that each
update step of the algorithm is adding one whole, continuous,
recognizable version of the basis sound.  It makes the evolution
of the approximation a wackier listening experience.

Also, after the first loop, you only need to recalculate
projections in Step (1) for starting positions that involve
some overlap with the interval `s', ..., s' + K - 1`.  That's
about `2K` positions instead of `N - K + 1`.

I've used the Akka library to parallelize the computation of
Step (1).  I did this because I think Akka is fun.  There's
a single `Conductor` actor in the system doing Steps (2),
(3), and (4).  It has a small army of `BasisFitter` actors,
one for each `N - K + 1` starting position.  The `Conductor`
sends residual snippets to each `BasisFitter`, waits to hear
back about how well each one's vector projection went, then
updates the residual accordingly and kicks it off again.

## Possible speedups to try.

It turns out to unfortunately take a while to run even a single
iteration.  A basis array of length `K` means doing `O(K^2)`
floating point multiplications each iteration of the loop.

It's certain to go faster if I rented a machine with like 32
CPU-equivalents, instead of my 4-core laptop.  Those cost
like two bucks an hour, plus the headache of working with
a remote machine.  The current WAVProjects I've put together
have definitely maxed out the available CPUs, so the speed
up is almost certainly there.

Instead of testing all starting positions in Step (1), I could
instead just pick a random 10 or 100 or 1000.  If I coupled that
with lazy evaluation of the vector projection, it might save
some time.  And the approximation would probably approach
the target audio at roughly the same speed.

In general, there's almost certainly a way I can cut
down on having to make new copies of `resid` snippets
all the time.  It'd break the nice little "shared nothing"
world my actors currently enjoy.

Right now, there's `N - K + 1` messages relayed back to
the `Conductor`, which processes them all one-by-one.
I could instead set up a prefix tree of other actors to
weed out suboptimal projection results tournament-style.
That could help parallelize Step (2) as well as Step (1).
(Though from my naive look at system resource utilization,
WAVProject seems like it's making use of 3 out of my 4
laptop cores, so it's probably spending all its time in
the already-parallelized Step (1) anyway.)

I could also try reimplenting this all in Numpy instead.
I just really enjoy writing in Scala, though.

## Related Work

https://www.youtube.com/watch?v=t-7mQhSZRgM

## Changelog

### Dec 14

Implemented "laziness" in the routine: moved the dot product
calculations back to the moment of a BasisFitRequest, and
enabled the Conductor to request only a subsample of basis
fitters for each step.

### Dec 1

Moved dot-product calculation in `BasisFitter` to a `while` loop.
Started passing residual snippets as arrays instead of lists.
(This fixed a goof-up where I was checking `.length` on a
`List`, which is expensive.)

[comment]: # (Second run)

[comment]: # (1 update: 10:01 PM)
[comment]: # (2 updates: 10:01 PM)
[comment]: # (4 updates: 10:01 PM)
[comment]: # (8 updates: 10:02 PM)
[comment]: # (16 updates: 10:02 PM)
[comment]: # (32 updates: 10:04 PM)
[comment]: # (64 updates: 10:06 PM)
[comment]: # (128 updates: 10:12 PM)
[comment]: # (256 updates: 10:23 PM)
[comment]: # (512 updates: 10:46 PM)
[comment]: # (1024 updates: 11:29 PM)

### Nov 28


[comment]: # (First run went slow:)

[comment]: # (1 update: 9:52 PM)
[comment]: # (2 updates: 9:54 PM)
[comment]: # (4 updates: 9:58 PM)
[comment]: # (8 updates: 10:04 PM)
[comment]: # (16 updates: 10:18 PM)
[comment]: # (32 updates: 10:43 PM)
[comment]: # (64 updates: 11:35 PM)
[comment]: # (128 updates: 1:15 AM)
[comment]: # (256 updates: 4:31 AM)
[comment]: # (512 updates: 11:10 AM)
[comment]: # (1024 updates: 12:16 AM)
