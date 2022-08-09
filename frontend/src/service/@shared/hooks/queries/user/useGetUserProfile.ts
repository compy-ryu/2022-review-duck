import { useQuery, UseQueryOptions } from 'react-query';

import { UserProfileResponse } from 'service/@shared/types';
import { ErrorResponse } from 'service/@shared/types';

import authAPI from 'service/@shared/api/auth';
import { PERMISSION_VALID_TIME, QUERY_KEY } from 'service/@shared/constants';

function useGetUserProfile(queryOptions?: UseQueryOptions<UserProfileResponse, ErrorResponse>) {
  return useQuery<UserProfileResponse, ErrorResponse>(
    [QUERY_KEY.DATA.USER, QUERY_KEY.API.GET_USER_PROFILE],
    () => authAPI.getProfile(),
    {
      suspense: true,
      useErrorBoundary: false,
      staleTime: PERMISSION_VALID_TIME,
      ...queryOptions,
    },
  );
}

export default useGetUserProfile;
