-module(client_behaviour).

-behaviour(gen_server).
-include("gen-erl/bank_schema_types.hrl").

%% API
-export([
  start_link/0
]).

-export([
  init/1,
  handle_call/3,
  handle_cast/2,
  handle_info/2,
  terminate/2,
  code_change/3]).

-define(SERVER, ?MODULE).

-record(state, {account_creator, standard_manager, premium_manager}).

start_link() ->
  gen_server:start_link({local, ?SERVER}, ?MODULE, [], []).

%%%===================================================================
%%% gen_server callbacks
%%%===================================================================

init([]) ->
  RegisterPort = 9099,
  ManagePort = 9091,
  Register = [
    {"register", account_creator_thrift}
  ],
  Manage = [
    {"standard", standard_manager_thrift},
    {"premium", premium_manager_thrift}
  ],

  {ok, [{"register", Account_Creator}]} = thrift_client_util:new_multiplexed("localhost", RegisterPort, Register, [{framed, true}]),

  {ok, [{"standard", Standard_Manager}, {"premium", Premium_Manager}]} = thrift_client_util:new_multiplexed("localhost", ManagePort, Manage, [{framed, true}]),

  {ok, #state{account_creator = Account_Creator, standard_manager = Standard_Manager, premium_manager = Premium_Manager}}.

terminate(_Reason, _State) ->
  ok.

handle_call({create_account, Args}, _From, State) ->
  try thrift_client:call(State#state.account_creator, 'registerClient', Args) of
    {_, {ok, Reply}} -> {reply, to_string(Reply), State};
    {_, {error, Error}} -> {reply, core_parse:format_error(Error), State}
  catch
    throw:{_, {_, Ex}} -> {reply, to_string(Ex), State}
  end;

handle_call({get_balance_standard, Args}, _From, State) ->
  try thrift_client:call(State#state.standard_manager, 'getBalance', Args) of
    {_, {ok, Reply}} -> {reply, to_string(Reply), State};
    {_, {error, Error}} -> {reply, core_parse:format_error(Error), State}
  catch
    throw:{_, {_, Ex}} -> {reply, to_string(Ex), State}
  end;

handle_call({get_balance_premium, Args}, _From, State) ->
  try thrift_client:call(State#state.premium_manager, 'getBalance', Args) of
    {_, {ok, Reply}} -> {reply, to_string(Reply), State};
    {_, {error, Error}} -> {reply, core_parse:format_error(Error), State}
  catch
    throw:{_, {_, Ex}} -> {reply, to_string(Ex), State}
  end;

handle_call({get_loan_cost, Args}, _From, State) ->
  try thrift_client:call(State#state.premium_manager, 'takeLoan', Args) of
    {_, {ok, Reply}} -> {reply, to_string(Reply), State};
    {_, {error, Error}} -> {reply, core_parse:format_error(Error), State}
  catch
    throw:{_, {_, Ex}} -> {reply, to_string(Ex), State}
  end;

handle_call(_Request, _From, State) ->
  {reply, ok, State}.

handle_cast(_Request, State) ->
  {noreply, State}.

handle_info(_Info, State) ->
  {noreply, State}.

code_change(_OldVsn, State, _Extra) ->
  {ok, State}.

%%%===================================================================
%%% Internal functions
%%%===================================================================

to_string(#'RegisterUserReponse'{passwd = Password, accountType = Type}) ->
  {AccType, _} = lists:keyfind(Type, 2, bank_schema_types:enum_info('AccountType')),
  erlang:iolist_to_binary(
    io_lib:format("~nYou Password:~s~nAccountType:~p~n", [
      Password#'Password'.password,
      AccType
    ])
  );

to_string(#'Money'{currency = Currency, amount = Amount}) ->
  {Curr, _} = lists:keyfind(Currency, 2, bank_schema_types:enum_info('Currency')),
  erlang:iolist_to_binary(
    io_lib:format("~.2f ~s~n", [
      Amount,
      Curr
    ])
  );

to_string(#'LoanResponse'{accepted = false}) ->
  io_lib:format("~n~nLoan has not been granted to you~n", []);

to_string(#'LoanResponse'{accepted = true, foreginCurrencyCost = F, nativeBankCurrencyCost = N}) ->
  erlang:iolist_to_binary(
    io_lib:format("~n~nLoanResponse:~n naive cost ~s foregin cost ~s~n", [
      to_string(N),
      to_string(F)
    ])
  );

to_string(#'ErrorInOperation'{reason = Reason}) ->
  erlang:iolist_to_binary(
    io_lib:format("~n~n****Exception:~n~s~n", [Reason])
  );

to_string(Reply) ->
  erlang:iolist_to_binary(
    io_lib:format("~n~nReply: ~p~n", [Reply])
  ).
