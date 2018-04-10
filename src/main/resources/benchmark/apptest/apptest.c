#include <stdio.h>
#include <unistd.h>

/**
 * Author : Oleg Lodygens (lodygens@lal.in2p3.fr)
 * Date   : Jan 22th, 2004
 *
 * This program is intended to be distributed
 * through an XtremWeb network.
 *
 * The only purpose is to test an xtremWeb network and to validate
 * the full computing process, including computing time 
 * (even if this program only sleeps), results size and I/O.
 *
 * Args : [-n <N>] to create a random binary file of N kb
 *                 (it is filled with random values so that
 *                  even the zipped result file is huge)
 *        [-s]     to sleep sum(2*N) seconds
 *        [-sq]    to sleep sum(N*N) seconds
 *        |-d]     to see how long this program would take accordingly to its params
 *                 (and immediatly exit)
 *
 * Examples:
 * 	$> void < myTextFile
 * 	$> void -n 4 < myTextFile
 * 	$> void -n 4 -s < myTextFile
# Copyrights     : CNRS
# Author         : Oleg Lodygensky
# Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
# Web            : http://www.xtremweb-hep.org
#
#      This file is part of XtremWeb-HEP.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#     http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
 */

#define ARRAYLENGTH 1024

void usage (char* name) {
  printf ("Usage :  %s [-n <N> [-s | -sq]] < <fileName>, where :\n", name);
  printf ("\t-n <N> to create a new file of N kb\n");
  printf ("\t-s     to sleep (2 * N) seconds\n");
  printf ("\t-sq    to sleep (N * N) seconds\n");
  printf ("\t<fileName> to dump the specified file\n");
}


long evaluate (int N, int square) {

  int i;
  long total = 0;

  for (i = 1; i <= N; i++) {
    if (square)
      total += (long)(i * i);
    else
      total += (long)(2 * i);
  }

  return total;
}


int main (int argc, char** argv) {
  FILE *fpin = NULL;
  FILE *fpout = NULL;
  long N = 0, i = 0, j = 0, arg = 1, delay = 0, sleeper = 0, square = 0;
  char myArray [ARRAYLENGTH];
  char *fileName = NULL;

  for (; arg < argc;) {

    if (!strcmp (argv [arg], "-h")) {
      usage (argv[0]);
      return 0;
    }

    if (!strcmp (argv [arg], "-d"))
      delay = 1;

    if (!strcmp (argv [arg], "-n")) {
      sscanf (argv[++arg], "%i", &N);
    }

    if (!strcmp (argv [arg], "-f"))
      fileName = argv [++arg];

    if (!strcmp (argv [arg], "-s"))
      sleeper = 1;
    if (!strcmp (argv [arg], "-sq")) {
      sleeper = 1;
      square = 1;
    }

    ++arg;
  }

  if (delay) {

    long total = evaluate (N, square);
    int heures = total / 3600;

    total -= heures * 3600;

    if (!square)
      printf ("\"%s -n %i -s \" would take : %li h %li mn %li s\n", argv[0], N, heures ,total / 60, total % 60);
    else
      printf ("\"%s -n %i -s -sq\" would take : %li h %li mn %li s\n", argv[0], N, heures ,total / 60, total % 60);

    return 0;
  }


  fpout = fopen ("TestResults.txt", "w+");
  if (fpout == NULL) {
    fprintf (stderr, "Cant create TestResults.txt\n");
    return 1;
  }

    while (!feof (stdin)) {
      if (fgets (myArray, ARRAYLENGTH, stdin) == NULL)
	break;
      printf ("%s", myArray);
      if (fpout != NULL)
	fprintf (fpout, "%s", myArray);
    }

    if (fpout != NULL)
      fclose (fpout);

    fflush (stdout);

  if (N <= 0) {
    fprintf (stderr, "N ==0\n");
    return 0;
  }


  fpout = fopen ("TestResults.bin", "w+");
  if (fpout == NULL) {
    fprintf (stderr, "Cant create TestResults.bin\n");
    return 1;
  }

  for (i = 1; i <= N; i++) {
    if (sleeper) {
      if (square)
	delay = (i * i);
      else
	delay = (2 * i);

      printf ("sleeping %li\n", delay);
      sleep (delay);
    }

    for (j = 0; j < ARRAYLENGTH; j++)
      myArray [j] = random(i) * i;

    fwrite (myArray, ARRAYLENGTH, 1, fpout);
    fflush (fpout);
  }

  fclose (fpout);

  printf ("done :)\n");

  return 0;
}
