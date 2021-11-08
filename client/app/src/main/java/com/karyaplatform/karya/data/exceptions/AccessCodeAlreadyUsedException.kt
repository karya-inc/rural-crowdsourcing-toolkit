// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.
//
// Exception raised when access code is already used

package com.karyaplatform.karya.data.exceptions

import com.karyaplatform.karya.R

class AccessCodeAlreadyUsedException : KaryaException(R.string.access_code_already_used)
