defmodule Hospital.Technic do
  use GenServer
  ## client API

  def start(spec1, spec2) do
    GenServer.start_link(__MODULE__, [{:spec1, spec1}, {:spec2, spec2}, {:spec4, "logs"}])
  end

  def stop(ref) do
    GenServer.stop(ref)
  end

  ## Server callbacks
  def terminate(_reason, state) do
    AMQP.Basic.cancel(state.channel, state.tag)
    AMQP.Basic.cancel(state.channel, state.tag1)
    AMQP.Basic.cancel(state.channel, state.tag2)
    AMQP.Channel.close(state.channel)
    AMQP.Connection.close(state.connection)
  end

  def init(opts) do
    {:ok, connection} = AMQP.Connection.open()
    {:ok, channel} = AMQP.Channel.open(connection)

    {:ok, _} = AMQP.Queue.declare(channel, opts[:spec4])
    {:ok, _} = AMQP.Queue.declare(channel, opts[:spec1])
    {:ok, _} = AMQP.Queue.declare(channel, opts[:spec2])

    {:ok, tag1} = AMQP.Basic.consume(channel, opts[:spec1])
    {:ok, tag2} = AMQP.Basic.consume(channel, opts[:spec2])

    AMQP.Exchange.declare(channel, "with_logs", :direct)

    for {_, q_name} <- opts do
      AMQP.Queue.bind(channel, q_name, "with_logs", routing_key: q_name)
      AMQP.Queue.bind(channel, opts[:spec4], "with_logs", routing_key: q_name)
    end


    AMQP.Exchange.declare(channel, "admin_says", :direct)
    {:ok, %{queue: queue_name}} = AMQP.Queue.declare(channel, "", exclusive: true)
    AMQP.Queue.bind(channel,queue_name,"admin_says",routing_key: "admin")

    {:ok, %{queue: queue_name}} = AMQP.Queue.declare(channel, "", exclusive: true)
    {:ok, tag} = AMQP.Basic.consume(channel, queue_name, nil, no_ack: true)

    {:ok,
     %{
       connection: connection,
       channel: channel,
       log_to: opts[:spec4],
       tag: tag,
       tag1: tag1,
       tag2: tag2
     }}
  end

  def handle_info({:basic_consume_ok, _msg}, state) do
    {:noreply, state}
  end

  def handle_info({:basic_deliver, msg, meta}, state) do
    IO.puts "technic recieved request"
    AMQP.Basic.publish(state.channel, "", meta.reply_to, "#{msg} done")
    AMQP.Basic.ack(state.channel, meta.delivery_tag)
    {:noreply, state}
  end
end
