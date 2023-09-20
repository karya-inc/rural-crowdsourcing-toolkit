// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.
//
// Process an input file for a task via CLI

import dotenv from 'dotenv';
dotenv.config();

import { knex, setupDbConnection, BasicModel } from '@karya/common';
import { Promise as BBPromise } from 'bluebird';
import { ChainedMicrotaskType } from '../chains/BackendChainInterface';
import { AssignmentRecordType } from '@karya/core';

const task_id = process.argv[2];

/** Main Script */
(async () => {
  setupDbConnection();

  // Get task
  const task = await BasicModel.getSingle('task', { id: task_id });
  if (task.scenario_name != 'SPEECH_VERIFICATION') {
    console.log('Invalid task id');
    return;
  }

  const mtas = (await BasicModel.getRecords('microtask_assignment', { task_id, status: 'COMPLETED' })) as Array<
    AssignmentRecordType<'SPEECH_VERIFICATION'>
  >;

  await BBPromise.mapSeries(mtas, async (mta) => {
    const mt = (await BasicModel.getSingle('microtask', { id: mta.microtask_id })) as ChainedMicrotaskType;
    const src_assignment_id = mt.input?.chain.assignmentId;
    if (!src_assignment_id) return;

    const output = mta.output;
    if (!output) return;
    if (output.data.auto) return;

    const accuracy = output.data.accuracy;
    const pay = accuracy == 2 ? 3 : 0;

    await BasicModel.updateSingle(
      'microtask_assignment',
      { id: src_assignment_id },
      { status: 'VERIFIED', verified_at: new Date().toISOString(), credits: pay }
    );
  });
})().finally(() => knex.destroy());
